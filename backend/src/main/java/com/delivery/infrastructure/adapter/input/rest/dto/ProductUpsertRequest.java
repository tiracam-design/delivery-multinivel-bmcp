package com.delivery.infrastructure.adapter.input.rest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO para la creación y edición de productos/platos del menú en el módulo Merchant.
 * Moneda: Dólares de los Estados Unidos ($ USD).
 *
 * @since 1.0.0
 */
public record ProductUpsertRequest(
        UUID merchantId,

        @NotBlank(message = "El nombre del plato es obligatorio")
        String name,

        String description,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0.00 USD")
        BigDecimal price,

        String category,

        String imageUrl,

        Boolean available
) {}
