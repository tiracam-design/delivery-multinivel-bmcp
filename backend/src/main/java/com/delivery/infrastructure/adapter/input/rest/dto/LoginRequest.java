package com.delivery.infrastructure.adapter.input.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Solicitud de inicio de sesión.
 *
 * @since 1.0.0
 */
public record LoginRequest(
        @NotBlank(message = "email es obligatorio")
        @Email(message = "Formato de email inválido")
        String email,

        @NotBlank(message = "password es obligatorio")
        String password
) {}
