/**
 * ═══════════════════════════════════════════════════════════════════════
 * INFRASTRUCTURE — Seguridad JWT
 * ═══════════════════════════════════════════════════════════════════════
 *
 * Configuración de Spring Security y componentes JWT:
 *   - JwtTokenProvider   → genera y valida tokens
 *   - JwtAuthFilter      → filtro que intercepta cada request
 *   - SecurityConfig     → configura cadena de filtros, CORS, CSRF
 *   - UserDetailsImpl    → implementación de UserDetails de Spring Security
 *
 * ✅ Anotados con @Component, @Configuration
 * ✅ Usa JJWT (io.jsonwebtoken) para firmar/verificar tokens
 */
package com.delivery.infrastructure.security;
