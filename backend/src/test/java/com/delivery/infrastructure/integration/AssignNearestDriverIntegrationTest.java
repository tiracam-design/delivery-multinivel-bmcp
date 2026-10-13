package com.delivery.infrastructure.integration;

import com.delivery.domain.model.Driver;
import com.delivery.domain.model.GeoLocation;
import com.delivery.domain.model.Order;
import com.delivery.domain.port.input.AssignNearestDriverUseCase;
import com.delivery.domain.port.input.command.AssignNearestDriverCommand;
import com.delivery.domain.port.output.NotificationPort;
import com.delivery.domain.port.output.OrderRepositoryPort;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test de Integración — Asignación del Repartidor Más Cercano.
 *
 * <p>Verifica el flujo completo end-to-end usando una base de datos
 * PostgreSQL + PostGIS real (vía Testcontainers) para validar que las
 * consultas geoespaciales {@code ST_DWithin} y {@code ST_Distance}
 * funcionan correctamente con datos reales.</p>
 *
 * <h3>Escenario de prueba (Tulcán, Carchi, Ecuador):</h3>
 * <pre>
 *   Punto de recogida (Parque Central Tulcán): lat=0.8116, lon=-77.7172
 *
 *   Driver A (Carlos)  → ~0.8 km al norte  → lat=0.8188  ✅ dentro del radio
 *   Driver B (María)   → ~2.5 km al sur    → lat=0.7891  ✅ dentro del radio
 *   Driver C (Pedro)   → ~10  km al norte  → lat=0.9016  ❌ fuera del radio
 *
 *   Radio de búsqueda: 3.0 km
 *   Resultado esperado: Driver A asignado (mismo nivel, menor distancia)
 * </pre>
 *
 * <h3>Infraestructura:</h3>
 * <ul>
 *   <li>Contenedor: {@code postgis/postgis:16-3.4} (PostgreSQL 16 + PostGIS 3.4)</li>
 *   <li>Esquema: gestionado por Flyway ({@code V1__init_schema.sql})</li>
 *   <li>Datos: insertados via native SQL respetando las FK del esquema</li>
 * </ul>
 *
 * @since 1.0.0
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("integration")
@Transactional
@DisplayName("Integración — AssignNearestDriverUseCase con PostGIS")
class AssignNearestDriverIntegrationTest {

    // ═══════════════════════════════════════════════════════════════
    //  TESTCONTAINER — PostgreSQL + PostGIS
    // ═══════════════════════════════════════════════════════════════

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("delivery_test")
            .withUsername("test_user")
            .withPassword("test_pass");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    // ═══════════════════════════════════════════════════════════════
    //  DEPENDENCIAS
    // ═══════════════════════════════════════════════════════════════

    @Autowired
    private AssignNearestDriverUseCase assignNearestDriverUseCase;

    @Autowired
    private OrderRepositoryPort orderRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * NotificationPort no tiene implementación en infraestructura aún.
     * Se mockea porque es un efecto secundario (notificación) que no
     * afecta la lógica geoespacial bajo prueba.
     */
    @MockBean
    private NotificationPort notificationPort;

    // ═══════════════════════════════════════════════════════════════
    //  CONSTANTES — Geometría de prueba
    // ═══════════════════════════════════════════════════════════════

    /** Punto de recogida: Parque de la Independencia, Tulcán, Carchi, Ecuador */
    private static final double PICKUP_LAT = 0.8116;
    private static final double PICKUP_LON = -77.7172;

    /**
     * Driver A — ~0.8 km al norte del Parque Central (Sector Parque Ayora / Cementerio de Tulcán).
     * Cálculo: Δlat ≈ 0.0072° → 0.0072 × 111.32 km/° ≈ 0.8 km
     */
    private static final double DRIVER_A_LAT = 0.8188;
    private static final double DRIVER_A_LON = -77.7172;

    /**
     * Driver B — ~2.5 km al sur del Parque Central (Sector Terminal Terrestre de Tulcán).
     * Cálculo: Δlat ≈ 0.0225° → 0.0225 × 111.32 km/° ≈ 2.5 km
     */
    private static final double DRIVER_B_LAT = 0.7891;
    private static final double DRIVER_B_LON = -77.7172;

    /**
     * Driver C — ~10 km al norte (Sector Puente Internacional de Rumichaca / Frontera).
     * Cálculo: Δlat ≈ 0.090° → 0.090 × 111.32 km/° ≈ 10.0 km
     */
    private static final double DRIVER_C_LAT = 0.9016;
    private static final double DRIVER_C_LON = -77.7172;

    /** Radio de búsqueda en kilómetros */
    private static final double SEARCH_RADIUS_KM = 3.0;

    // ═══════════════════════════════════════════════════════════════
    //  FIXTURE — IDs de entidades insertadas
    // ═══════════════════════════════════════════════════════════════

    private UUID driverAId;   // ~1 km  — DEBE ser asignado
    private UUID driverBId;   // ~2.5 km — dentro del radio pero más lejos
    private UUID driverCId;   // ~10 km — fuera del radio
    private UUID orderId;

    // ═══════════════════════════════════════════════════════════════
    //  SETUP — Inserción de datos de prueba via SQL nativo
    // ═══════════════════════════════════════════════════════════════

    @BeforeEach
    void setUp() {
        // ── 1. Crear usuarios (respetando FK de tablas dependientes) ─
        UUID customerUserId = insertUser("cliente@test.com",
                "Cliente Test", "CUSTOMER");
        UUID merchantOwnerId = insertUser("merchant@test.com",
                "Dueño Comercio", "MERCHANT");
        UUID driverUserAId = insertUser("driver.a@test.com",
                "Carlos García", "DRIVER");
        UUID driverUserBId = insertUser("driver.b@test.com",
                "María López", "DRIVER");
        UUID driverUserCId = insertUser("driver.c@test.com",
                "Pedro Sánchez", "DRIVER");

        // ── 2. Crear comercio con ubicación PostGIS ─────────────────
        UUID merchantId = insertMerchant(merchantOwnerId,
                "Tacos El Güero", "Calle República 45, Centro",
                PICKUP_LAT, PICKUP_LON);

        // ── 3. Crear 3 repartidores (JUNIOR, disponibles, con GPS) ──
        driverAId = insertDriver(driverUserAId, "JUNIOR",
                DRIVER_A_LAT, DRIVER_A_LON);    // ~1.0 km
        driverBId = insertDriver(driverUserBId, "JUNIOR",
                DRIVER_B_LAT, DRIVER_B_LON);    // ~2.5 km
        driverCId = insertDriver(driverUserCId, "JUNIOR",
                DRIVER_C_LAT, DRIVER_C_LON);    // ~10 km

        // ── 4. Crear pedido con pickup location en Tulcán ───────────
        orderId = insertOrder(customerUserId, merchantId,
                "Calle Sucre 14-20 y Pichincha, Parque Central, Tulcán",
                PICKUP_LAT, PICKUP_LON,
                "Av. Coral 450 y Av. Manabí, Terminal Terrestre, Tulcán",
                0.8035, -77.7245);

        // Sincronizar el persistence context con la BD para que
        // las native queries PostGIS vean los datos insertados
        entityManager.flush();
        entityManager.clear();
    }

    // ═══════════════════════════════════════════════════════════════
    //  TESTS — Asignación con radio de 3 km
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Asignación con radio de 3 km")
    class AssignWithRadius3Km {

        @Test
        @DisplayName("Debe asignar al repartidor más cercano (~1 km) dentro del radio")
        void shouldAssignClosestDriverWithinRadius() {
            // Arrange
            var command = new AssignNearestDriverCommand(orderId, SEARCH_RADIUS_KM);

            // Act
            Optional<Driver> result = assignNearestDriverUseCase.execute(command);

            // Assert — Se encontró un repartidor
            assertTrue(result.isPresent(),
                    "Debe encontrar al menos un repartidor dentro de 3 km");

            Driver assignedDriver = result.get();

            // Assert — El repartidor asignado es Driver A (el más cercano)
            assertEquals(driverAId, assignedDriver.getId(),
                    "Debe asignar al repartidor más cercano (Driver A ≈ 1 km)");

            // Assert — La distancia del asignado es < 3 km
            assertNotNull(assignedDriver.getDistanceKm(),
                    "El repartidor debe tener distancia calculada por PostGIS");
            assertTrue(assignedDriver.getDistanceKm() < SEARCH_RADIUS_KM,
                    String.format(
                            "Distancia del asignado (%.2f km) debe ser < radio (%.1f km)",
                            assignedDriver.getDistanceKm(), SEARCH_RADIUS_KM));

            // Assert — La distancia está en el rango esperado (~1 km ± 0.2)
            assertTrue(assignedDriver.getDistanceKm() >= 0.8
                            && assignedDriver.getDistanceKm() <= 1.2,
                    String.format(
                            "Distancia de Driver A debe ser ≈ 1.0 km, fue %.4f km",
                            assignedDriver.getDistanceKm()));
        }

        @Test
        @DisplayName("Debe persistir la asignación del repartidor en la orden")
        void shouldPersistDriverAssignment() {
            // Arrange
            var command = new AssignNearestDriverCommand(orderId, SEARCH_RADIUS_KM);

            // Act
            assignNearestDriverUseCase.execute(command);

            // Forzar re-lectura desde BD (no cache de primer nivel)
            entityManager.flush();
            entityManager.clear();

            // Assert — La orden en BD tiene el driver asignado
            Order savedOrder = orderRepository.findById(orderId)
                    .orElseThrow(() -> new AssertionError(
                            "La orden debe existir en la BD"));

            assertAll("Persistencia de asignación",
                    () -> assertNotNull(savedOrder.getDriverId(),
                            "La orden debe tener un repartidor asignado"),
                    () -> assertEquals(driverAId, savedOrder.getDriverId(),
                            "El driverId persistido debe ser Driver A"),
                    () -> assertTrue(savedOrder.hasDriverAssigned(),
                            "hasDriverAssigned() debe retornar true")
            );
        }

        @Test
        @DisplayName("No debe asignar al repartidor que está a 10 km (fuera del radio)")
        void shouldExcludeDriverOutsideRadius() {
            // Arrange
            var command = new AssignNearestDriverCommand(orderId, SEARCH_RADIUS_KM);

            // Act
            Optional<Driver> result = assignNearestDriverUseCase.execute(command);

            // Assert
            assertTrue(result.isPresent());
            assertNotEquals(driverCId, result.get().getId(),
                    "No debe asignar a Driver C (~10 km, fuera del radio de 3 km)");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  TESTS — Escenarios sin candidatos
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Escenarios sin candidatos")
    class NoCandidatesScenarios {

        @Test
        @DisplayName("Debe retornar vacío si no hay repartidores dentro del radio")
        void shouldReturnEmptyWhenNoDriversInRadius() {
            // Arrange — radio muy pequeño (0.1 km = 100 metros)
            var command = new AssignNearestDriverCommand(orderId, 0.1);

            // Act
            Optional<Driver> result = assignNearestDriverUseCase.execute(command);

            // Assert
            assertTrue(result.isEmpty(),
                    "No debe encontrar repartidores dentro de 100 metros");

            // La orden NO debe tener repartidor asignado
            entityManager.flush();
            entityManager.clear();
            Order order = orderRepository.findById(orderId).orElseThrow();
            assertFalse(order.hasDriverAssigned(),
                    "La orden NO debe tener repartidor si no hay candidatos");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  TESTS — Prioridad multinivel
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Prioridad multinivel")
    class MultiLevelPriority {

        @Test
        @DisplayName("Debe preferir al SENIOR sobre el JUNIOR aunque esté más lejos")
        void shouldPreferHigherLevelDriver() {
            // Arrange — Promover Driver B a SENIOR (está a ~2.5 km)
            entityManager.createNativeQuery(
                            "UPDATE drivers SET delivery_level = 'SENIOR' WHERE id = :id")
                    .setParameter("id", driverBId)
                    .executeUpdate();
            entityManager.flush();
            entityManager.clear();

            var command = new AssignNearestDriverCommand(orderId, SEARCH_RADIUS_KM);

            // Act
            Optional<Driver> result = assignNearestDriverUseCase.execute(command);

            // Assert — Driver B (SENIOR a ~2.5 km) tiene prioridad
            //          sobre Driver A (JUNIOR a ~1 km)
            assertTrue(result.isPresent(),
                    "Debe encontrar repartidores dentro de 3 km");
            assertEquals(driverBId, result.get().getId(),
                    "Debe asignar al SENIOR (Driver B ≈ 2.5 km) sobre el "
                            + "JUNIOR (Driver A ≈ 1 km), porque el nivel tiene "
                            + "prioridad sobre la distancia");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPERS — Inserción de datos via SQL nativo PostGIS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Inserta un usuario en la tabla {@code users} usando SQL nativo.
     * <p>Genera un UUID único en Java para poder referenciarlo luego.</p>
     *
     * @return UUID del usuario insertado
     */
    private UUID insertUser(String email, String fullName, String role) {
        UUID id = UUID.randomUUID();
        // Agregar sufijo aleatorio al email para evitar colisiones
        // entre tests que se ejecutan en la misma transacción
        String uniqueEmail = email.replace("@", "-" + id.toString().substring(0, 8) + "@");

        entityManager.createNativeQuery("""
                        INSERT INTO users (id, email, password_hash, full_name, phone, role, active, created_at, updated_at)
                        VALUES (:id, :email, :hash, :name, :phone, :role, TRUE, NOW(), NOW())
                        """)
                .setParameter("id", id)
                .setParameter("email", uniqueEmail)
                .setParameter("hash", "$2a$10$dummyHashForTestingPurposes")
                .setParameter("name", fullName)
                .setParameter("phone", "+52 55 1234 5678")
                .setParameter("role", role)
                .executeUpdate();

        return id;
    }

    /**
     * Inserta un comercio con ubicación PostGIS (ST_SetSRID + ST_MakePoint).
     *
     * @return UUID del merchant insertado
     */
    private UUID insertMerchant(UUID ownerId, String name, String address,
                                double lat, double lon) {
        UUID id = UUID.randomUUID();

        entityManager.createNativeQuery("""
                        INSERT INTO merchants (id, owner_id, name, address, location, active, created_at, updated_at)
                        VALUES (:id, :ownerId, :name, :address,
                                ST_SetSRID(ST_MakePoint(:lon, :lat), 4326),
                                TRUE, NOW(), NOW())
                        """)
                .setParameter("id", id)
                .setParameter("ownerId", ownerId)
                .setParameter("name", name)
                .setParameter("address", address)
                .setParameter("lon", lon)
                .setParameter("lat", lat)
                .executeUpdate();

        return id;
    }

    /**
     * Inserta un repartidor con ubicación actual PostGIS.
     * <p>Usa {@code ST_SetSRID(ST_MakePoint(lon, lat), 4326)} para crear
     * la geometría del punto directamente en SQL nativo, garantizando
     * compatibilidad total con las queries de {@code ST_DWithin}.</p>
     *
     * @return UUID del driver insertado
     */
    private UUID insertDriver(UUID userId, String level,
                               double lat, double lon) {
        UUID id = UUID.randomUUID();

        entityManager.createNativeQuery("""
                        INSERT INTO drivers (id, user_id, delivery_level, vehicle_plate, vehicle_type,
                                             current_location, available, rating, total_deliveries,
                                             created_at, updated_at)
                        VALUES (:id, :userId, :level, :plate, 'MOTORCYCLE',
                                ST_SetSRID(ST_MakePoint(:lon, :lat), 4326),
                                TRUE, 0.00, 0, NOW(), NOW())
                        """)
                .setParameter("id", id)
                .setParameter("userId", userId)
                .setParameter("level", level)
                .setParameter("plate", "TST-" + id.toString().substring(0, 4))
                .setParameter("lon", lon)
                .setParameter("lat", lat)
                .executeUpdate();

        return id;
    }

    /**
     * Inserta un pedido en estado {@code CREATED} con ubicaciones PostGIS.
     * <p>Usa el puerto {@link OrderRepositoryPort} para crear el pedido a
     * través de la capa de dominio, garantizando que el PIN anti-fraude,
     * historial de estados y todas las invariantes se generen correctamente.</p>
     *
     * @return UUID del pedido creado
     */
    private UUID insertOrder(UUID customerId, UUID merchantId,
                              String pickupAddress, double pickupLat, double pickupLon,
                              String deliveryAddress, double deliveryLat, double deliveryLon) {
        GeoLocation pickupLocation = new GeoLocation(pickupLat, pickupLon);
        GeoLocation deliveryLocation = new GeoLocation(deliveryLat, deliveryLon);

        Order order = Order.create(
                customerId,
                merchantId,
                pickupAddress,
                pickupLocation,
                deliveryAddress,
                deliveryLocation,
                new BigDecimal("150.50"),
                "Pedido de prueba — test de integración PostGIS"
        );

        Order savedOrder = orderRepository.save(order);
        return savedOrder.getId();
    }
}
