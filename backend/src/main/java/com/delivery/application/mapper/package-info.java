/**
 * ═══════════════════════════════════════════════════════════════════════
 * CAPA APPLICATION — Mappers (Domain ↔ DTO)
 * ═══════════════════════════════════════════════════════════════════════
 *
 * Interfaces MapStruct que transforman entre modelos de dominio y DTOs
 * de aplicación. Se procesan en compile-time (sin reflexión en runtime).
 *
 * ✅ Anotados con @Mapper(componentModel = "spring")
 * ✅ Métodos puros de conversión
 *
 * Convención: [Entidad]Mapper
 *   Ejemplos: OrderMapper, UserMapper, DeliveryMapper
 */
package com.delivery.application.mapper;
