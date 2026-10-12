/**
 * ═══════════════════════════════════════════════════════════════════════
 * INFRASTRUCTURE — Adapter In: REST Controllers
 * ═══════════════════════════════════════════════════════════════════════
 *
 * Adaptadores de entrada HTTP. Reciben peticiones REST, las transforman
 * en comandos/queries de aplicación e invocan los Input Ports.
 *
 * ✅ Anotados con @RestController, @RequestMapping
 * ✅ Dependen SOLO de Input Ports (interfaces de domain.port.input)
 * ✅ Delegan toda la lógica al caso de uso correspondiente
 * ❌ SIN lógica de negocio
 *
 * Convención: [Entidad]Controller
 *   Ejemplos: OrderController, UserController, DeliveryController
 */
package com.delivery.infrastructure.adapter.input.rest;
