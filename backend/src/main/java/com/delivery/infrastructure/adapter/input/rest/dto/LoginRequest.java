package com.delivery.infrastructure.adapter.input.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Solicitud de inicio de sesión.
 *
 * @since 1.0.0
 */
public record LoginRequest(
        @NotBlank(message = "El email o nombre de usuario es obligatorio")
        String email,

        @NotBlank(message = "El password es obligatorio")
        String password
) {}
