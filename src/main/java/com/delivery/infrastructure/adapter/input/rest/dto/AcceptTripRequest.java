package com.delivery.infrastructure.adapter.input.rest.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * DTO para la aceptación de viaje por parte del repartidor.
 *
 * @since 1.0.0
 */
public record AcceptTripRequest(
        @NotNull(message = "driverId es obligatorio")
        UUID driverId
) {}
