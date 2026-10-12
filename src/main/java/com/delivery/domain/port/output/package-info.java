/**
 * ═══════════════════════════════════════════════════════════════════════
 * CAPA DOMAIN — Puertos de Salida (Output Ports / Driven Ports)
 * ═══════════════════════════════════════════════════════════════════════
 *
 * Interfaces que definen las dependencias EXTERNAS que el dominio necesita
 * pero NO implementa. Son implementadas por la capa de Infrastructure
 * (Adapters de Salida: JPA Repositories, Clients HTTP, Servicios de Correo).
 *
 * ❌ SIN anotaciones de Spring
 * ✅ Contratos puros — definen el "QUÉ" necesita el dominio del exterior
 *
 * Convención de nombres: [Sustantivo]Repository | [Sustantivo]Gateway
 *   Ejemplos: OrderRepository, UserRepository, PaymentGateway,
 *             NotificationGateway, GeocodingGateway
 */
package com.delivery.domain.port.output;
