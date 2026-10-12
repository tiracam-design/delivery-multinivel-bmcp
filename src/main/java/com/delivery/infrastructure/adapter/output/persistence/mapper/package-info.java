/**
 * ═══════════════════════════════════════════════════════════════════════
 * INFRASTRUCTURE — Adapter Out: Persistencia JPA — Mappers (Entity ↔ Domain)
 * ═══════════════════════════════════════════════════════════════════════
 *
 * MapStruct mappers que convierten entre entidades JPA y modelos de dominio.
 * Se usan exclusivamente dentro de los PersistenceAdapter.
 *
 * ✅ @Mapper(componentModel = "spring")
 *
 * Convención: [Entidad]PersistenceMapper
 *   Ejemplos: UserPersistenceMapper, OrderPersistenceMapper
 */
package com.delivery.infrastructure.adapter.output.persistence.mapper;
