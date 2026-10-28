package com.delivery.infrastructure.adapter.output.persistence.repository;

import com.delivery.infrastructure.adapter.output.persistence.entity.BankAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para la entidad {@link BankAccountEntity}.
 *
 * @since 1.0.0
 */
@Repository
public interface BankAccountJpaRepository extends JpaRepository<BankAccountEntity, UUID> {

    Optional<BankAccountEntity> findByMerchantId(UUID merchantId);

    Optional<BankAccountEntity> findByDriverId(UUID driverId);

    Optional<BankAccountEntity> findByUserId(UUID userId);

    Optional<BankAccountEntity> findFirstByUserIdOrderByUpdatedAtDesc(UUID userId);
}
