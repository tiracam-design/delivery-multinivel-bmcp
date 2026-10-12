package com.delivery.infrastructure.adapter.input.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO de respuesta con la información del pedido.
 *
 * @since 1.0.0
 */
public record OrderResponse(
        UUID id,
        UUID customerId,
        UUID merchantId,
        UUID driverId,
        String status,
        String pickupAddress,
        String deliveryAddress,
        Double estimatedDistanceKm,
        Integer estimatedDurationMinutes,
        BigDecimal totalAmount,
        String deliveryPin,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {}
