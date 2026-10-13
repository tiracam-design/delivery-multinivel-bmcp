package com.delivery.infrastructure.adapter.output.persistence.repository;

import com.delivery.infrastructure.adapter.output.persistence.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para la entidad {@link OrderEntity}.
 *
 * <p>Proporciona operaciones CRUD auto-generadas y queries personalizados.
 * Este repositorio es usado exclusivamente por el
 * {@code OrderPersistenceAdapter} — nunca es inyectado directamente
 * en la capa de application o domain.</p>
 *
 * @since 1.0.0
 */
@Repository
public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {

    List<OrderEntity> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    List<OrderEntity> findByDriverIdAndStatusOrderByCreatedAtAsc(UUID driverId, String status);

    List<OrderEntity> findByStatusOrderByCreatedAtAsc(String status);

    @Modifying
    @Query("UPDATE OrderEntity o SET o.status = :status, o.updatedAt = CURRENT_INSTANT WHERE o.id = :orderId")
    int updateStatusById(@Param("orderId") UUID orderId, @Param("status") String status);

    /**
     * Calcula la distancia geodésica exacta en km entre dos puntos WGS 84 usando PostGIS.
     */
    @Query(nativeQuery = true, value = """
            SELECT ST_Distance(
                ST_SetSRID(ST_MakePoint(:originLon, :originLat), 4326)::geography,
                ST_SetSRID(ST_MakePoint(:destLon, :destLat), 4326)::geography
            ) / 1000.0
            """)
    Double calculateDistanceKm(@Param("originLon") double originLon,
                               @Param("originLat") double originLat,
                               @Param("destLon") double destLon,
                               @Param("destLat") double destLat);
}
