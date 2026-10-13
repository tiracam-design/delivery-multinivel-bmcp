package com.delivery.infrastructure.adapter.input.rest.dto;

import java.util.UUID;

/**
 * DTO de respuesta con la información del repartidor asignado.
 *
 * @since 1.0.0
 */
public record DriverResponse(
        UUID id,
        String fullName,
        String phone,
        String level,
        Double distanceKm
) {}
