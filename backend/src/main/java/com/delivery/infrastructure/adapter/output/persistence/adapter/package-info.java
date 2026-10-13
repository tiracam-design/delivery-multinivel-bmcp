/**
 * ═══════════════════════════════════════════════════════════════════════
 * INFRASTRUCTURE — Adapter Out: Persistencia JPA — Adaptadores
 * ═══════════════════════════════════════════════════════════════════════
 *
 * Implementaciones concretas de los Output Ports del dominio.
 * Actúan como puente entre el dominio y Spring Data JPA:
 *   1. Reciben llamadas del dominio/aplicación (vía Output Port)
 *   2. Convierten modelos de dominio → entidades JPA
 *   3. Delegan en los JpaRepository
 *   4. Convierten entidades JPA → modelos de dominio para la respuesta
 *
 * ✅ Anotados con @Component / @Repository
 * ✅ Implementan interfaces de domain.port.output
 * ✅ Inyectan los JpaRepository y Mappers de persistencia
 *
 * Convención: [Entidad]PersistenceAdapter
 *   Ejemplos: UserPersistenceAdapter, OrderPersistenceAdapter
 */
package com.delivery.infrastructure.adapter.output.persistence.adapter;
