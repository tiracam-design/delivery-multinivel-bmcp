package com.delivery.modules.admin.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MerchantApprovalResponseDTO(
        UUID id,
        String name,
        String address,
        String phone,
        String email,
        String category,
        BigDecimal rating,
        boolean active,
        boolean rucValid,
        Instant createdAt
) {}
