package com.delivery.infrastructure.adapter.input.rest.dto;

import java.util.UUID;

/**
 * Respuesta del endpoint de autenticación con token JWT y perfil de usuario.
 *
 * @since 1.0.0
 */
public record AuthResponse(
        String token,
        String tokenType,
        long expiresInMs,
        UUID userId,
        String email,
        String fullName,
        String role
) {
    public static AuthResponse of(String token, long expiresInMs, UUID userId, String email, String fullName, String role) {
        return new AuthResponse(token, "Bearer", expiresInMs, userId, email, fullName, role);
    }
}
