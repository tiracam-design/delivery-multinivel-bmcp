package com.delivery.infrastructure.adapter.output.persistence.repository;

import com.delivery.infrastructure.adapter.output.persistence.entity.DriverLocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para la tabla {@code driver_locations}.
 * Historial de tracking GPS de repartidores.
 *
 * @since 1.0.0
 */
@Repository
public interface DriverLocationJpaRepository
        extends JpaRepository<DriverLocationEntity, UUID> {

    /**
     * Busca el registro de ubicación más reciente de un repartidor.
     *
     * @param driverId ID del repartidor
     * @return último registro de ubicación, o vacío
     */
    @Query("""
            SELECT dl FROM DriverLocationEntity dl
            WHERE dl.driverId = :driverId
            ORDER BY dl.recordedAt DESC
            LIMIT 1
            """)
    Optional<DriverLocationEntity> findLatestByDriverId(
            @Param("driverId") UUID driverId);
}
