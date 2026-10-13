package com.delivery.modules.merchant.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record KitchenOrderResponseDTO(
        UUID orderId,
        String status,
        String customerName,
        String deliveryAddress,
        BigDecimal totalAmount,
        String notes,
        Instant createdAt
) {}
