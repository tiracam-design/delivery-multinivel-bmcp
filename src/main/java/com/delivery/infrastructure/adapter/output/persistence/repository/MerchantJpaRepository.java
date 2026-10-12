package com.delivery.infrastructure.adapter.output.persistence.repository;

import com.delivery.infrastructure.adapter.output.persistence.entity.MerchantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para la entidad MerchantEntity.
 *
 * @since 1.0.0
 */
@Repository
public interface MerchantJpaRepository extends JpaRepository<MerchantEntity, UUID> {

    /**
     * Busca comercios pendientes de aprobación por parte de la administración.
     */
    List<MerchantEntity> findByActiveFalse();
}
