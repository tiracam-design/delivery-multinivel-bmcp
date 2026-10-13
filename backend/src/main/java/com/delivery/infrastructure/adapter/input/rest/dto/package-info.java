/**
 * ═══════════════════════════════════════════════════════════════════════
 * INFRASTRUCTURE — Adapter In: REST — DTOs de Request/Response
 * ═══════════════════════════════════════════════════════════════════════
 *
 * Objetos que modelan el contrato JSON de la API REST.
 * Son específicos de la capa HTTP y pueden diferir de los DTOs internos
 * de la capa de aplicación.
 *
 * ✅ Records Java con anotaciones Jackson (@JsonProperty, etc.)
 * ✅ Validaciones Jakarta (@NotBlank, @Email, @Positive, etc.)
 *
 * Subcarpetas sugeridas:
 *   request/   → payloads de entrada (CreateOrderRequest, LoginRequest)
 *   response/  → payloads de respuesta (OrderResponse, TokenResponse)
 */
package com.delivery.infrastructure.adapter.input.rest.dto;
