package com.delivery.infrastructure.adapter.input.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * DTO de entrada para la validación del PIN de entrega.
 *
 * @since 1.0.0
 */
public record ValidateDeliveryPinRequest(

        @NotNull(message = "orderId es obligatorio")
        UUID orderId,

        @NotBlank(message = "pin es obligatorio")
        String pin
) {}
