package com.delivery.infrastructure.adapter.input.rest.dto;

import com.delivery.infrastructure.adapter.output.persistence.entity.ProductEntity;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO de respuesta para productos y platos del menú del comercio.
 *
 * @since 1.0.0
 */
public record ProductResponseDTO(
        UUID id,
        UUID merchantId,
        String name,
        String description,
        BigDecimal price,
        String category,
        String imageUrl,
        boolean available,
        String createdAt,
        String updatedAt
) {
    public static ProductResponseDTO fromEntity(ProductEntity entity) {
        return new ProductResponseDTO(
                entity.getId(),
                entity.getMerchantId(),
                entity.getName(),
                entity.getDescription(),
                entity.getPrice(),
                entity.getCategory() != null ? entity.getCategory() : "Platos Fuertes",
                entity.getImageUrl(),
                entity.isAvailable(),
                entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null,
                entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null
        );
    }
}
