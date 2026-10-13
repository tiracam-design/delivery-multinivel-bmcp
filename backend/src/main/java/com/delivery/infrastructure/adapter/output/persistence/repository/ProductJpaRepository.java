package com.delivery.infrastructure.adapter.output.persistence.repository;

import com.delivery.infrastructure.adapter.output.persistence.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para la entidad {@link ProductEntity}.
 *
 * @since 1.0.0
 */
@Repository
public interface ProductJpaRepository extends JpaRepository<ProductEntity, UUID> {

    List<ProductEntity> findByMerchantIdOrderByNameAsc(UUID merchantId);

    @Modifying
    @Query("UPDATE ProductEntity p SET p.available = NOT p.available, p.updatedAt = CURRENT_INSTANT WHERE p.id = :productId")
    int toggleAvailabilityById(@Param("productId") UUID productId);
}
