package com.delivery.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración de Spring Security con arquitectura Stateless y autenticación JWT.
 *
 * <p>Asegura el control de acceso basado en roles (RBAC):</p>
 * <ul>
 *   <li><strong>HTTP 401 Unauthorized:</strong> cuando el token JWT no se provee o es inválido/expirado
 *       ({@link JwtAuthenticationEntryPoint}).</li>
 *   <li><strong>HTTP 403 Forbidden:</strong> cuando un usuario autenticado intenta acceder a un recurso
 *       no autorizado para su rol (ej: CLIENT accediendo a endpoints de ADMIN) ({@link JwtAccessDeniedHandler}).</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                          JwtAccessDeniedHandler jwtAccessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtAccessDeniedHandler = jwtAccessDeniedHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Habilitar CORS y desactivar CSRF para APIs REST stateless
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)

                // Sesiones stateless (sin cookies de sesión HTTP)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Manejo de errores HTTP 401 y HTTP 403 estructurado en JSON
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                )

                // Reglas de autorización por ruta y roles
                .authorizeHttpRequests(auth -> auth
                        // Rutas públicas: autenticación, documentación Swagger y WebSockets
                        .requestMatchers(
                                "/v1/auth/**",
                                "/auth/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/ws-delivery/**",
                                "/ws-delivery",
                                "/error"
                        ).permitAll()

                        // Rutas exclusivas de Administrador (HTTP 403 si el rol no es ADMIN)
                        .requestMatchers(
                                "/v1/admin/**",
                                "/admin/**"
                        ).hasRole("ADMIN")

                        // Rutas de Repartidor
                        .requestMatchers(
                                "/v1/driver/**",
                                "/driver/**"
                        ).hasAnyRole("DRIVER", "ADMIN")

                        // Rutas de Comercio / Restaurante
                        .requestMatchers(
                                "/v1/merchant/**",
                                "/merchant/**",
                                "/v1/merchants/**",
                                "/merchants/**"
                        ).hasAnyRole("MERCHANT", "ADMIN")

                        // Rutas de Pedidos y Clientes
                        .requestMatchers(
                                "/v1/client/**",
                                "/client/**",
                                "/v1/orders/**",
                                "/orders/**"
                        ).hasAnyRole("CLIENT", "CUSTOMER", "DRIVER", "MERCHANT", "ADMIN")

                        // Cualquier otra petición debe estar autenticada
                        .anyRequest().authenticated()
                )

                // Desactivar autenticación básica y login interactivo por defecto
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // Registrar filtro JWT antes del filtro de autenticación por usuario/contraseña
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
