package com.delivery.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de Spring Security.
 *
 * <p>Habilita la seguridad web y la seguridad a nivel de métodos
 * ({@code @PreAuthorize}) para el control de acceso basado en roles (RBAC).</p>
 *
 * @since 1.0.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Desactivar CSRF para APIs REST (stateless)
                .csrf(AbstractHttpConfigurer::disable)

                // Sesiones stateless (preparado para JWT)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Autorización de rutas
                .authorizeHttpRequests(auth -> auth
                        // Swagger UI y OpenAPI docs — acceso público
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // Todos los endpoints — acceso público (desarrollo)
                        // TODO: Restringir por roles cuando se implemente JWT
                        .anyRequest().permitAll()
                )

                // Desactivar el formulario de login por defecto
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
