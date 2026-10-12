/**
 * ═══════════════════════════════════════════════════════════════════════
 * INFRASTRUCTURE — Adapter Out: Persistencia JPA — Repositorios Spring
 * ═══════════════════════════════════════════════════════════════════════
 *
 * Interfaces que extienden JpaRepository / JpaSpecificationExecutor.
 * Proporcionan acceso CRUD y queries derivados o nativos (PostGIS).
 *
 * ✅ Extienden JpaRepository<XEntity, Long>
 * ✅ Queries geoespaciales con @Query nativo (ST_DWithin, ST_Distance)
 *
 * Convención: [Entidad]JpaRepository
 *   Ejemplos: UserJpaRepository, OrderJpaRepository
 */
package com.delivery.infrastructure.adapter.output.persistence.repository;
