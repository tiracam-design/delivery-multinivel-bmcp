package com.delivery.modules.driver.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ActiveTripResponseDTO(
        UUID orderId,
        String status,
        String restaurantName,
        String pickupAddress,
        String deliveryAddress,
        String customerName,
        String customerPhone,
        BigDecimal totalAmount,
        Double estimatedDistanceKm,
        Integer estimatedDurationMinutes,
        String notes,
        Instant acceptedAt
) {}
