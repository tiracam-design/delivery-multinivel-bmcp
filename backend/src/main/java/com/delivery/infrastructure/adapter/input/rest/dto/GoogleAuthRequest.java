package com.delivery.infrastructure.adapter.input.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Solicitud de autenticación Google OAuth")
public record GoogleAuthRequest(
        @NotBlank(message = "El correo electrónico es requerido")
        @Email(message = "Formato de correo electrónico inválido")
        @Schema(example = "usuario.google@tulcan.ec")
        String email,

        @Schema(example = "Usuario Google")
        String fullName,

        @Schema(example = "CLIENT", description = "Rol objetivo: CLIENT, DRIVER, MERCHANT, ADMIN")
        String role,

        @Schema(example = "google-oauth2|1029384756")
        String googleId
) {}
