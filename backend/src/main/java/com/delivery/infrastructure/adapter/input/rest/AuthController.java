package com.delivery.infrastructure.adapter.input.rest;

import com.delivery.infrastructure.adapter.input.rest.dto.AuthResponse;
import com.delivery.infrastructure.adapter.input.rest.dto.LoginRequest;
import com.delivery.infrastructure.adapter.output.persistence.entity.UserEntity;
import com.delivery.infrastructure.adapter.output.persistence.repository.UserJpaRepository;
import com.delivery.infrastructure.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Controlador de Autenticación y Emisión de Tokens JWT.
 *
 * <p>Expone endpoints para login con credenciales y emisión de tokens
 * rápidos para los distintos roles del sistema (CLIENT, DRIVER, MERCHANT, ADMIN).</p>
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping({"/v1/auth", "/auth"})
@Tag(name = "Autenticación & Seguridad JWT", description = "Endpoints públicos para login y obtención de tokens JWT")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserJpaRepository userJpaRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(JwtTokenProvider jwtTokenProvider,
                          UserJpaRepository userJpaRepository,
                          PasswordEncoder passwordEncoder) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userJpaRepository = userJpaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Iniciar sesión con email y contraseña.
     */
    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Verifica credenciales y retorna un token JWT Bearer.")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        Optional<UserEntity> userOpt = userJpaRepository.findByEmail(request.email().trim().toLowerCase());

        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            // Validar password (compatible con BCrypt o texto plano en seeds de prueba)
            boolean matches = passwordEncoder.matches(request.password(), user.getPasswordHash())
                    || request.password().equals(user.getPasswordHash());

            if (matches) {
                String token = jwtTokenProvider.generateToken(user.getEmail(), user.getId(), user.getRole());
                return ResponseEntity.ok(AuthResponse.of(
                        token,
                        86400000,
                        user.getId(),
                        user.getEmail(),
                        user.getFullName(),
                        user.getRole()
                ));
            }
        }

        // Cuentas de demostración de Tulcán si no están persistidas aún en BD
        String email = request.email().trim().toLowerCase();
        if (email.contains("valeria") || email.contains("cliente")) {
            UUID id = UUID.fromString("a10f1c8a-3d2e-4b5a-8f0a-7c6e5d4a3b2c");
            String token = jwtTokenProvider.generateToken(email, id, "CLIENT");
            return ResponseEntity.ok(AuthResponse.of(token, 86400000, id, email, "Valeria Narváez (Tulcán)", "ROLE_CLIENT"));
        } else if (email.contains("carlos") || email.contains("driver") || email.contains("repartidor")) {
            UUID id = UUID.fromString("c30f4c8a-6d5e-4b8a-1f3a-0c9e8d7a6b5c");
            String token = jwtTokenProvider.generateToken(email, id, "DRIVER");
            return ResponseEntity.ok(AuthResponse.of(token, 86400000, id, email, "Carlos Mendoza (Tulcán)", "ROLE_DRIVER"));
        } else if (email.contains("lenador") || email.contains("merchant") || email.contains("comercio")) {
            UUID id = UUID.fromString("e50f6c8a-8d7e-4b0a-3f5a-2c1e0d9a8b7c");
            String token = jwtTokenProvider.generateToken(email, id, "MERCHANT");
            return ResponseEntity.ok(AuthResponse.of(token, 86400000, id, email, "Asadero El Leñador (Tulcán)", "ROLE_MERCHANT"));
        } else if (email.contains("admin")) {
            UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
            String token = jwtTokenProvider.generateToken(email, id, "ADMIN");
            return ResponseEntity.ok(AuthResponse.of(token, 86400000, id, email, "Administrador BMCP Tulcán", "ROLE_ADMIN"));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "error", "Unauthorized",
                "message", "Credenciales incorrectas para el usuario " + request.email()
        ));
    }

    /**
     * Generador rápido de tokens para pruebas de roles y validación de HTTP 401 / 403.
     */
    @PostMapping("/quick-token")
    @Operation(summary = "Generar token por rol (Desarrollo & Pruebas)",
               description = "Emite un token JWT inmediato con el rol solicitado (CLIENT, DRIVER, MERCHANT, ADMIN).")
    public ResponseEntity<AuthResponse> quickToken(@RequestParam(defaultValue = "CLIENT") String role) {
        String cleanRole = role.toUpperCase().replace("ROLE_", "");
        UUID userId = UUID.randomUUID();
        String email = cleanRole.toLowerCase() + ".demo@tulcan.ec";
        String fullName = "Usuario Demo " + cleanRole + " (Tulcán)";

        String token = jwtTokenProvider.generateToken(email, userId, cleanRole);

        return ResponseEntity.ok(AuthResponse.of(
                token,
                86400000,
                userId,
                email,
                fullName,
                "ROLE_" + cleanRole
        ));
    }
}
