package com.delivery.modules.client.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderTrackingResponseDTO(
        UUID orderId,
        String status,
        String deliveryPin,
        String merchantName,
        String pickupAddress,
        String deliveryAddress,
        String driverName,
        String driverPhone,
        String driverLevel,
        BigDecimal totalAmount,
        String currency,
        Double driverLatitude,
        Double driverLongitude,
        Instant updatedAt
) {}
