/**
 * ═══════════════════════════════════════════════════════════════════════
 * CAPA APPLICATION — Servicios / Casos de Uso
 * ═══════════════════════════════════════════════════════════════════════
 *
 * Implementaciones concretas de los Input Ports (casos de uso).
 * Orquestan la lógica de negocio invocando modelos de dominio y
 * delegando en Output Ports para efectos secundarios (persistencia,
 * notificaciones, pagos, etc.).
 *
 * ✅ Anotados con @Service (gestión transaccional por Spring)
 * ✅ Dependen SOLO de interfaces de domain.port (input y output)
 * ❌ SIN dependencia directa de JPA, HTTP o infraestructura concreta
 *
 * Convención de nombres: [Verbo][Sustantivo]Service
 *   Ejemplos: CreateOrderService, AssignDeliveryService,
 *             RegisterUserService, CalculateRouteService
 */
package com.delivery.application.service;
