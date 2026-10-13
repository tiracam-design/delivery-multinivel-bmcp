package com.delivery.infrastructure.adapter.input.rest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/**
 * DTO de entrada para la asignación del repartidor más cercano.
 *
 * @since 1.0.0
 */
public record AssignDriverRequest(

        @NotNull(message = "orderId es obligatorio")
        UUID orderId,

        @Positive(message = "radiusKm debe ser positivo")
        Double radiusKm
) {
    public double getRadiusKmOrDefault() {
        return radiusKm != null ? radiusKm : 5.0;
    }
}
