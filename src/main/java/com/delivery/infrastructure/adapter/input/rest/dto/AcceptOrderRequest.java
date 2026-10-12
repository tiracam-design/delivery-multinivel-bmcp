package com.delivery.infrastructure.adapter.input.rest.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO de solicitud para que el comercio acepte un pedido e indique
 * el tiempo estimado de preparación en cocina.
 *
 * @since 1.0.0
 */
public record AcceptOrderRequest(
        @NotNull(message = "preparationMinutes es obligatorio")
        @Min(value = 1, message = "El tiempo mínimo de preparación es de 1 minuto")
        @Max(value = 180, message = "El tiempo máximo de preparación es de 180 minutos")
        Integer preparationMinutes
) {}
