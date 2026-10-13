/**
 * ═══════════════════════════════════════════════════════════════════════
 * INFRASTRUCTURE — Adapter Out: Persistencia JPA — Entidades
 * ═══════════════════════════════════════════════════════════════════════
 *
 * Entidades JPA/Hibernate que mapean las tablas de PostgreSQL.
 * Son específicas de la infraestructura y NO se exponen al dominio.
 *
 * ✅ Anotadas con @Entity, @Table, @Column, @Id
 * ✅ Pueden usar Hibernate Spatial (@Type, Point, Polygon)
 * ✅ Se convierten a/desde modelos de dominio via mappers
 *
 * Convención: [Entidad]Entity
 *   Ejemplos: UserEntity, OrderEntity, DeliveryRouteEntity
 */
package com.delivery.infrastructure.adapter.output.persistence.entity;
