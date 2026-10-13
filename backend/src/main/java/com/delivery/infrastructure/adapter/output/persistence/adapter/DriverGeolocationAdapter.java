package com.delivery.infrastructure.adapter.output.persistence.adapter;

import com.delivery.domain.enums.DeliveryLevel;
import com.delivery.domain.model.Driver;
import com.delivery.domain.model.GeoLocation;
import com.delivery.domain.port.output.DriverGeolocationPort;
import com.delivery.infrastructure.adapter.output.persistence.entity.DriverLocationEntity;
import com.delivery.infrastructure.adapter.output.persistence.mapper.OrderPersistenceMapper;
import com.delivery.infrastructure.adapter.output.persistence.repository.DriverJpaRepository;
import com.delivery.infrastructure.adapter.output.persistence.repository.DriverJpaRepository.DriverWithDistanceProjection;
import com.delivery.infrastructure.adapter.output.persistence.repository.DriverLocationJpaRepository;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Adaptador de salida que implementa {@link DriverGeolocationPort}
 * usando PostgreSQL/PostGIS para todas las operaciones geoespaciales.
 *
 * <h3>Arquitectura de datos de ubicación:</h3>
 * <pre>
 *   ┌─────────────────────────────────────────────────────────┐
 *   │                    WebSocket / REST                     │
 *   │              (posición GPS del repartidor)              │
 *   └──────────────┬──────────────────────────────────────────┘
 *                  │
 *                  ▼
 *   ┌──────────────────────────────┐   ┌───────────────────────┐
 *   │    drivers.current_location  │   │   driver_locations     │
 *   │    (última posición conocida)│   │   (historial completo) │
 *   │    → Usada por ST_DWithin    │   │   → Auditoría, replay  │
 *   │    → Actualización in-place  │   │   → INSERT append-only │
 *   └──────────────────────────────┘   └───────────────────────┘
 * </pre>
 *
 * <h3>Estrategia dual de escritura:</h3>
 * <ol>
 *   <li><strong>UPDATE</strong> {@code drivers.current_location} — la posición
 *       "última conocida" usada por las queries de asignación (hot path)</li>
 *   <li><strong>INSERT</strong> en {@code driver_locations} — registro
 *       histórico append-only para auditoría y replay de rutas</li>
 * </ol>
 *
 * @since 1.0.0
 */
@Component
@Transactional(readOnly = true)
public class DriverGeolocationAdapter implements DriverGeolocationPort {

    private static final Logger LOG = Logger.getLogger(
            DriverGeolocationAdapter.class.getName());

    /** Metros por kilómetro — factor de conversión para ST_DWithin */
    private static final double METERS_PER_KM = 1_000.0;

    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    private final DriverJpaRepository driverRepository;
    private final DriverLocationJpaRepository locationRepository;

    public DriverGeolocationAdapter(DriverJpaRepository driverRepository,
                                    DriverLocationJpaRepository locationRepository) {
        this.driverRepository = Objects.requireNonNull(driverRepository);
        this.locationRepository = Objects.requireNonNull(locationRepository);
    }

    // ════════════════════════════════════════════════════════════════
    //  BÚSQUEDA ESPACIAL — ST_DWithin + ST_Distance
    // ════════════════════════════════════════════════════════════════

    /**
     * {@inheritDoc}
     *
     * <h3>Flujo de ejecución PostGIS:</h3>
     * <pre>
     *   1. ST_DWithin(driver::geography, center::geography, radiusMeters)
     *      → Filtro índice-aware sobre GIST: descarta O(n) candidatos en O(log n)
     *
     *   2. ST_Distance(driver::geography, center::geography) / 1000.0
     *      → Distancia geodésica exacta en km (elipsoide WGS 84)
     *
     *   3. JOIN users → trae full_name, phone del perfil
     *
     *   4. ORDER BY distance_km ASC → más cercanos primero
     * </pre>
     *
     * <h3>Rendimiento esperado:</h3>
     * <ul>
     *   <li>Con índice GIST: ~1-5ms para 10K repartidores</li>
     *   <li>Sin índice GIST: ~50-200ms (full table scan)</li>
     * </ul>
     */
    @Override
    public List<Driver> findAvailableDriversWithinRadius(GeoLocation center,
                                                         double radiusKm) {
        Objects.requireNonNull(center, "center no puede ser null");
        if (radiusKm <= 0) {
            throw new IllegalArgumentException(
                    "radiusKm debe ser positivo, recibido: " + radiusKm);
        }

        double radiusMeters = radiusKm * METERS_PER_KM;

        LOG.fine(() -> String.format(
                "[GEO] Buscando repartidores dentro de %.1f km "
                        + "desde [lat=%.6f, lon=%.6f] (radio=%,.0f m)",
                radiusKm, center.getLatitude(), center.getLongitude(),
                radiusMeters));

        List<DriverWithDistanceProjection> projections =
                driverRepository.findAvailableDriversWithinRadius(
                        center.getLongitude(),  // x = longitude
                        center.getLatitude(),   // y = latitude
                        radiusMeters
                );

        LOG.fine(() -> String.format(
                "[GEO] Encontrados %d repartidores dentro del radio",
                projections.size()));

        return projections.stream()
                .map(this::toDomainDriver)
                .toList();
    }

    // ════════════════════════════════════════════════════════════════
    //  ACTUALIZACIÓN DE UBICACIÓN — Dual Write
    // ════════════════════════════════════════════════════════════════

    /**
     * {@inheritDoc}
     *
     * <h3>Estrategia dual de escritura:</h3>
     * <ol>
     *   <li><strong>UPDATE</strong> {@code drivers.current_location}
     *       — posición actual para queries de asignación</li>
     *   <li><strong>INSERT</strong> en {@code driver_locations}
     *       — registro histórico con heading y speed</li>
     * </ol>
     */
    @Override
    @Transactional
    public void updateDriverLocation(UUID driverId, GeoLocation location,
                                     Double heading, Double speed) {
        Objects.requireNonNull(driverId, "driverId no puede ser null");
        Objects.requireNonNull(location, "location no puede ser null");

        // ── 1. UPDATE posición actual en tabla drivers ───────────────
        int updated = driverRepository.updateCurrentLocation(
                driverId,
                location.getLongitude(),
                location.getLatitude()
        );

        if (updated == 0) {
            LOG.warning(() -> String.format(
                    "[GEO] No se encontró el repartidor id=%s para actualizar ubicación",
                    driverId));
            return;
        }

        // ── 2. INSERT registro histórico en driver_locations ────────
        DriverLocationEntity historyRecord = new DriverLocationEntity();
        historyRecord.setDriverId(driverId);
        historyRecord.setLocation(toPoint(location));
        historyRecord.setHeading(heading);
        historyRecord.setSpeed(speed);
        historyRecord.setRecordedAt(Instant.now());

        locationRepository.save(historyRecord);

        LOG.fine(() -> String.format(
                "[GEO] Ubicación actualizada. driverId=%s, lat=%.6f, lon=%.6f, "
                        + "heading=%s°, speed=%s km/h",
                driverId, location.getLatitude(), location.getLongitude(),
                heading != null ? String.format("%.0f", heading) : "N/A",
                speed != null ? String.format("%.1f", speed) : "N/A"));
    }

    // ════════════════════════════════════════════════════════════════
    //  ÚLTIMA UBICACIÓN CONOCIDA
    // ════════════════════════════════════════════════════════════════

    /**
     * {@inheritDoc}
     *
     * <p>Busca primero en el historial de ubicaciones ({@code driver_locations})
     * por el registro más reciente. Si no hay historial, retorna {@code null}.</p>
     */
    @Override
    public GeoLocation findLatestLocation(UUID driverId) {
        Objects.requireNonNull(driverId, "driverId no puede ser null");

        return locationRepository.findLatestByDriverId(driverId)
                .map(entity -> {
                    Point p = entity.getLocation();
                    return new GeoLocation(p.getY(), p.getX());
                })
                .orElse(null);
    }

    // ════════════════════════════════════════════════════════════════
    //  CONVERSIONES PRIVADAS
    // ════════════════════════════════════════════════════════════════

    /**
     * Convierte la proyección de la query PostGIS al modelo de dominio Driver.
     *
     * <p>La proyección retorna las coordenadas separadas (lat, lon) y la
     * distancia ya calculada por PostGIS, evitando recalcular con Haversine.</p>
     */
    private Driver toDomainDriver(DriverWithDistanceProjection projection) {
        GeoLocation location = null;
        if (projection.getLatitude() != null && projection.getLongitude() != null) {
            location = new GeoLocation(
                    projection.getLatitude(),
                    projection.getLongitude()
            );
        }

        return new Driver(
                projection.getDriverId(),
                projection.getFullName(),
                projection.getPhone(),
                DeliveryLevel.valueOf(projection.getDeliveryLevel()),
                location,
                projection.getAvailable(),
                projection.getDistanceKm()
        );
    }

    /**
     * Convierte GeoLocation del dominio a JTS Point con SRID 4326.
     */
    private Point toPoint(GeoLocation location) {
        Point point = GEOMETRY_FACTORY.createPoint(
                new Coordinate(location.getLongitude(), location.getLatitude()));
        point.setSRID(4326);
        return point;
    }
}
