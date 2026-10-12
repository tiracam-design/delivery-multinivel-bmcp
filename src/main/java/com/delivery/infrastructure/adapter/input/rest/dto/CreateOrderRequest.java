package com.delivery.infrastructure.adapter.input.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO de entrada para la creación de pedidos.
 *
 * @since 1.0.0
 */
public record CreateOrderRequest(

        @NotNull(message = "customerId es obligatorio")
        UUID customerId,

        @NotNull(message = "merchantId es obligatorio")
        UUID merchantId,

        @NotBlank(message = "pickupAddress es obligatorio")
        String pickupAddress,

        Double pickupLatitude,
        Double pickupLongitude,

        @NotBlank(message = "deliveryAddress es obligatorio")
        String deliveryAddress,

        Double deliveryLatitude,
        Double deliveryLongitude,

        @NotNull(message = "totalAmount es obligatorio")
        @Positive(message = "totalAmount debe ser positivo")
        BigDecimal totalAmount,

        String notes
) {}
