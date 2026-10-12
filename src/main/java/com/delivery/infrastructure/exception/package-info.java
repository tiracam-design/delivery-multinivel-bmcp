/**
 * ═══════════════════════════════════════════════════════════════════════
 * INFRASTRUCTURE — Manejo Global de Excepciones
 * ═══════════════════════════════════════════════════════════════════════
 *
 * @ControllerAdvice que captura excepciones de dominio y las traduce
 * a respuestas HTTP estandarizadas (RFC 7807 Problem Details).
 *
 * ✅ Mapea excepciones de domain.exception → HTTP Status + body JSON
 * ✅ Maneja ConstraintViolationException, MethodArgumentNotValidException
 */
package com.delivery.infrastructure.exception;
