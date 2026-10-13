/**
 * ═══════════════════════════════════════════════════════════════════════
 * INFRASTRUCTURE — Adapter Out: PostGIS / Geoespacial
 * ═══════════════════════════════════════════════════════════════════════
 *
 * Adaptadores especializados en operaciones geoespaciales con PostGIS.
 * Encapsulan queries nativos que usan funciones como ST_DWithin,
 * ST_Distance, ST_MakeEnvelope, ST_Contains.
 *
 * ✅ Implementan Output Ports geoespaciales del dominio
 * ✅ Pueden usar EntityManager directo para queries nativos complejos
 *
 * Convención: [Feature]GeoAdapter
 *   Ejemplos: DeliveryZoneGeoAdapter, DriverLocationGeoAdapter
 */
package com.delivery.infrastructure.adapter.output.postgis;
