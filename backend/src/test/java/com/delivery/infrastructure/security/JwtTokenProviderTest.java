package com.delivery.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias — JwtTokenProvider")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String secretKey = "ClaveSecretaDePruebaParaHMACSHA256ConMasDe256BitsDeLargo12345";
    private final long expirationMs = 3600000; // 1 hora

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(secretKey, expirationMs);
    }

    @Test
    @DisplayName("Debe generar y validar un token JWT correctamente")
    void shouldGenerateAndValidateToken() {
        UUID userId = UUID.randomUUID();
        String username = "valeria.narvaez@tulcan.ec";
        String role = "CLIENT";

        String token = jwtTokenProvider.generateToken(username, userId, role);

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals(username, jwtTokenProvider.getUsernameFromToken(token));
        assertEquals("ROLE_CLIENT", jwtTokenProvider.getRoleFromToken(token));
        assertEquals(userId, jwtTokenProvider.getUserIdFromToken(token));
    }

    @Test
    @DisplayName("Debe rechazar un token manipulado o con firma inválida")
    void shouldRejectTamperedToken() {
        String token = jwtTokenProvider.generateToken("user@tulcan.ec", UUID.randomUUID(), "ADMIN");
        String tamperedToken = token + "corrupted";

        assertFalse(jwtTokenProvider.validateToken(tamperedToken));
    }

    @Test
    @DisplayName("Debe rechazar un token expirado")
    void shouldRejectExpiredToken() throws InterruptedException {
        // Proveedor con 1 ms de expiración
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(secretKey, 1);
        String token = shortLivedProvider.generateToken("expired@tulcan.ec", UUID.randomUUID(), "DRIVER");

        Thread.sleep(10);

        assertFalse(shortLivedProvider.validateToken(token));
    }
}
