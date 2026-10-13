package com.delivery.infrastructure.adapter.output.persistence.repository;

import com.delivery.infrastructure.adapter.output.persistence.entity.DriverEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para consultas geoespaciales PostGIS
 * sobre repartidores.
 *
 * <h3>Estrategia de consultas espaciales:</h3>
 * <ul>
 *   <li>{@code ST_DWithin(geography, geography, meters)} — filtro por radio
 *       que aprovecha el índice GIST para descarte rápido (O(log n))</li>
 *   <li>{@code ST_Distance(geography, geography)} — cálculo de distancia
 *       geodésica real en metros (elipsoide WGS 84)</li>
 *   <li>El cast a {@code ::geography} es <strong>obligatorio</strong> para
 *       que PostGIS calcule distancias en metros sobre la esfera terrestre
 *       en lugar de grados cartesianos</li>
 * </ul>
 *
 * <h3>¿Por qué ST_DWithin y no ST_Distance &lt; radio?</h3>
 * <p>{@code ST_DWithin} es una función de <strong>índice-aware</strong>:
 * PostGIS puede usar el índice GIST para pre-filtrar candidatos antes
 * de calcular la distancia exacta. {@code ST_Distance < X} fuerza un
 * full table scan.</p>
 *
 * @since 1.0.0
 */
@Repository
public interface DriverJpaRepository extends JpaRepository<DriverEntity, UUID> {

    /**
     * Encuentra repartidores disponibles dentro de un radio desde un punto
     * de referencia, ordenados por distancia ascendente.
     *
     * <h3>Query PostGIS explicada:</h3>
     * <pre>
     *  1. ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)
     *     → Crea un punto geométrico WGS 84 desde las coordenadas
     *
     *  2. ST_DWithin(
     *       d.current_location::geography,
     *       punto_ref::geography,
     *       :radiusMeters
     *     )
     *     → Filtro espacial que usa el índice GIST.
     *       El cast a geography convierte de grados a metros.
     *       :radiusMeters = km * 1000
     *
     *  3. ST_Distance(
     *       d.current_location::geography,
     *       punto_ref::geography
     *     ) / 1000.0 AS distance_km
     *     → Distancia geodésica exacta en km (divide metros / 1000)
     *
     *  4. JOIN users u ON d.user_id = u.id
     *     → Trae full_name y phone del perfil de usuario
     *
     *  5. ORDER BY distance_km ASC
     *     → Los más cercanos primero
     * </pre>
     *
     * @param longitude longitud del punto de referencia (x en PostGIS)
     * @param latitude  latitud del punto de referencia (y en PostGIS)
     * @param radiusMeters radio de búsqueda en <strong>metros</strong>
     * @return proyecciones de repartidores con distancia calculada
     */
    @Query(nativeQuery = true, value = """
            SELECT
                d.id              AS driver_id,
                u.full_name       AS full_name,
                u.phone           AS phone,
                d.delivery_level  AS delivery_level,
                ST_Y(d.current_location)  AS latitude,
                ST_X(d.current_location)  AS longitude,
                d.available       AS available,
                ST_Distance(
                    d.current_location::geography,
                    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
                ) / 1000.0       AS distance_km
            FROM drivers d
            JOIN users u ON d.user_id = u.id
            WHERE d.available = TRUE
              AND d.current_location IS NOT NULL
              AND u.active = TRUE
              AND ST_DWithin(
                    d.current_location::geography,
                    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                    :radiusMeters
                  )
            ORDER BY distance_km ASC
            """)
    List<DriverWithDistanceProjection> findAvailableDriversWithinRadius(
            @Param("longitude") double longitude,
            @Param("latitude") double latitude,
            @Param("radiusMeters") double radiusMeters
    );

    /**
     * Actualiza la ubicación GPS actual del repartidor en la tabla drivers.
     *
     * <p>Esta es la posición "última conocida" usada por las consultas
     * de asignación. El historial completo se guarda en driver_locations.</p>
     *
     * @param driverId ID del repartidor
     * @param longitude nueva longitud
     * @param latitude nueva latitud
     * @return filas afectadas (0 si el driver no existe)
     */
    @Modifying
    @Query(nativeQuery = true, value = """
            UPDATE drivers
            SET current_location = ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326),
                updated_at = NOW()
            WHERE id = :driverId
            """)
    int updateCurrentLocation(
            @Param("driverId") UUID driverId,
            @Param("longitude") double longitude,
            @Param("latitude") double latitude
    );

    /**
     * Encuentra todos los repartidores pendientes de aprobación (active = false).
     */
    List<DriverEntity> findByActiveFalse();

    /**
     * Proyección de interfaz para el resultado de la consulta espacial.
     *
     * <p>Spring Data JPA mapea automáticamente los alias SQL a los
     * getters de esta interfaz (interface-based projection).</p>
     */
    interface DriverWithDistanceProjection {

        UUID getDriverId();

        String getFullName();

        String getPhone();

        String getDeliveryLevel();

        Double getLatitude();

        Double getLongitude();

        Boolean getAvailable();

        Double getDistanceKm();
    }
}
