/**
 * ═══════════════════════════════════════════════════════════════════════
 * CAPA DOMAIN — Puertos de Entrada (Input Ports / Driving Ports)
 * ═══════════════════════════════════════════════════════════════════════
 *
 * Interfaces que definen los CASOS DE USO que el sistema ofrece.
 * Son implementadas por la capa de Application (Use Cases / Services).
 * Son invocadas por los Adapters de Entrada (REST Controllers, WebSocket).
 *
 * ❌ SIN anotaciones de Spring
 * ✅ Contratos puros del dominio — definen el "QUÉ" hace el sistema
 *
 * Convención de nombres: [Verbo][Sustantivo]UseCase
 *   Ejemplos: CreateOrderUseCase, AssignDeliveryUseCase,
 *             RegisterUserUseCase, CalculateRouteUseCase
 */
package com.delivery.domain.port.input;
