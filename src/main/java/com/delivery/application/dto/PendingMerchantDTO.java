package com.delivery.application.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO que representa un comercio pendiente de aprobación administrativa.
 *
 * @since 1.0.0
 */
public record PendingMerchantDTO(
        UUID id,
        String name,
        String description,
        String address,
        String phone,
        String email,
        String category,
        boolean active,
        Instant createdAt
) {}
