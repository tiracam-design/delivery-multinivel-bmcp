package com.delivery.modules.admin.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DriverApprovalResponseDTO(
        UUID id,
        UUID userId,
        String deliveryLevel,
        String vehiclePlate,
        String vehicleType,
        String licenseNumber,
        String taxId,
        boolean active,
        boolean taxIdValid,
        BigDecimal rating,
        int totalDeliveries,
        Instant createdAt
) {}
