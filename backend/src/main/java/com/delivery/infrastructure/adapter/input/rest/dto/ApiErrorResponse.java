package com.delivery.infrastructure.adapter.input.rest.dto;

import java.time.Instant;

/**
 * DTO de respuesta estándar para errores (RFC 7807 simplificado).
 *
 * @since 1.0.0
 */
public record ApiErrorResponse(
        String code,
        String message,
        int status,
        Instant timestamp
) {
    public ApiErrorResponse(String code, String message, int status) {
        this(code, message, status, Instant.now());
    }
}
