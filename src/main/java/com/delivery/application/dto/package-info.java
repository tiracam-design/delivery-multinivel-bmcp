/**
 * ═══════════════════════════════════════════════════════════════════════
 * CAPA APPLICATION — DTOs de Entrada/Salida
 * ═══════════════════════════════════════════════════════════════════════
 *
 * Objetos de transferencia que desacoplan los datos que entran y salen
 * de los casos de uso respecto a la forma del modelo de dominio o
 * la API REST.
 *
 * ✅ Records Java o POJOs simples con validación (@NotNull, @Size, etc.)
 * ❌ SIN lógica de negocio
 *
 * Subcarpetas sugeridas:
 *   command/  → DTOs de escritura (CreateOrderCommand, RegisterUserCommand)
 *   query/    → DTOs de lectura (OrderSummaryResponse, UserProfileResponse)
 */
package com.delivery.application.dto;
