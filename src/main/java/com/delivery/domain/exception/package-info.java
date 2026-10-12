/**
 * ═══════════════════════════════════════════════════════════════════════
 * CAPA DOMAIN — Excepciones de Dominio
 * ═══════════════════════════════════════════════════════════════════════
 *
 * Excepciones tipadas que expresan violaciones a las reglas de negocio.
 * Son lanzadas desde la capa de dominio o aplicación y capturadas
 * en infraestructura (p.ej. GlobalExceptionHandler) para mapearlas
 * a respuestas HTTP.
 *
 * ❌ SIN dependencias de Spring
 * ✅ Extienden RuntimeException o una base de dominio propia
 *
 * Ejemplos: OrderNotFoundException, InvalidDeliveryLevelException,
 *           InsufficientBalanceException, ZoneNotCoveredException
 */
package com.delivery.domain.exception;
