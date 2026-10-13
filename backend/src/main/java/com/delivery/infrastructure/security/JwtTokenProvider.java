package com.delivery.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Proveedor de servicios criptográficos para generación, validación y
 * extracción de Claims en tokens JWT (JSON Web Tokens).
 *
 * <p>Implementado bajo la especificación JJWT 0.12.x con algoritmo HMAC-SHA256.</p>
 *
 * @since 1.0.0
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${security.jwt.secret-key:CAMBIAR_EN_PRODUCCION_clave_secreta_256_bits_minimo}") String secretKey,
            @Value("${security.jwt.expiration-ms:86400000}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * Genera un nuevo token JWT firmado.
     *
     * @param username correo o identificador del usuario
     * @param userId   UUID único del usuario
     * @param role     rol de seguridad (ej: CLIENT, DRIVER, MERCHANT, ADMIN)
     * @return cadena JWT compactada
     */
    public String generateToken(String username, UUID userId, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        String normalizedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId != null ? userId.toString() : UUID.randomUUID().toString())
                .claim("role", normalizedRole)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * Valida la firma criptográfica y la vigencia temporal del token.
     *
     * @param token cadena JWT
     * @return true si el token es íntegro y vigente; false si está expirado o alterado
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("❌ [SECURITY] Token JWT inválido o expirado: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Extrae todos los claims contenidos en el payload del token.
     */
    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extrae el nombre de usuario (subject) del token.
     */
    public String getUsernameFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    /**
     * Extrae el rol asignado al usuario.
     */
    public String getRoleFromToken(String token) {
        return getClaimsFromToken(token).get("role", String.class);
    }

    /**
     * Extrae el identificador UUID del usuario.
     */
    public UUID getUserIdFromToken(String token) {
        String idStr = getClaimsFromToken(token).get("userId", String.class);
        return idStr != null ? UUID.fromString(idStr) : null;
    }
}
