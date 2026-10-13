package com.delivery.infrastructure.adapter.input.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO para la validación del PIN de 4 dígitos en el punto de entrega.
 *
 * @since 1.0.0
 */
public record DriverValidatePinRequest(
        @NotBlank(message = "El PIN es obligatorio")
        @Pattern(regexp = "^\\d{4}$", message = "El PIN debe tener exactamente 4 dígitos numéricos")
        String pin
) {}
