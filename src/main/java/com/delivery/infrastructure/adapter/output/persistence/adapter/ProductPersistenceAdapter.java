package com.delivery.infrastructure.adapter.output.persistence.adapter;

import com.delivery.domain.model.Product;
import com.delivery.domain.port.output.ProductRepositoryPort;
import com.delivery.infrastructure.adapter.output.persistence.entity.ProductEntity;
import com.delivery.infrastructure.adapter.output.persistence.repository.ProductJpaRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de salida que implementa {@link ProductRepositoryPort}
 * usando Spring Data JPA.
 *
 * @since 1.0.0
 */
@Component
@Transactional(readOnly = true)
public class ProductPersistenceAdapter implements ProductRepositoryPort {

    private final ProductJpaRepository jpaRepository;

    public ProductPersistenceAdapter(ProductJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Product> findById(UUID productId) {
        return jpaRepository.findById(productId).map(this::toDomain);
    }

    @Override
    @Transactional
    public Product save(Product product) {
        ProductEntity entity = toEntity(product);
        ProductEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional
    public boolean toggleAvailability(UUID productId) {
        int updated = jpaRepository.toggleAvailabilityById(productId);
        if (updated == 0) {
            throw new IllegalArgumentException("Producto no encontrado con ID: " + productId);
        }
        return jpaRepository.findById(productId)
                .map(ProductEntity::isAvailable)
                .orElse(false);
    }

    @Override
    public List<Product> findByMerchantId(UUID merchantId) {
        return jpaRepository.findByMerchantIdOrderByNameAsc(merchantId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    // ── Mapeadores ──────────────────────────────────────────────────

    private Product toDomain(ProductEntity entity) {
        if (entity == null) return null;
        return new Product(
                entity.getId(),
                entity.getMerchantId(),
                entity.getName(),
                entity.getDescription(),
                entity.getPrice(),
                entity.getCategory(),
                entity.getImageUrl(),
                entity.isAvailable(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private ProductEntity toEntity(Product domain) {
        if (domain == null) return null;
        ProductEntity entity = new ProductEntity();
        entity.setId(domain.getId());
        entity.setMerchantId(domain.getMerchantId());
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setPrice(domain.getPrice());
        entity.setCategory(domain.getCategory());
        entity.setImageUrl(domain.getImageUrl());
        entity.setAvailable(domain.isAvailable());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
