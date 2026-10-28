package com.delivery.infrastructure.adapter.output.persistence.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * Convertidor JPA para aplicar cifrado sutil y transparente (AES-128) a nivel de columna
 * en campos sensibles como número de cuenta bancaria e identificación del titular.
 *
 * <p>Diseñado con compatibilidad hacia atrás: los registros preexistentes en texto plano
 * se leen sin error y se cifran automáticamente al ser actualizados.</p>
 *
 * @since 1.0.0
 */
@Converter
public class SensitiveDataCryptoConverter implements AttributeConverter<String, String> {

    private static final Logger log = LoggerFactory.getLogger(SensitiveDataCryptoConverter.class);
    private static final String PREFIX = "ENC:";
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    // Clave simétrica derivada de forma determinista para persistencia segura
    private static final byte[] KEY_BYTES;

    static {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] key = sha.digest("BMCP-Delivery-Secure-Salt-Tulcan-2026".getBytes(StandardCharsets.UTF_8));
            KEY_BYTES = Arrays.copyOf(key, 16); // AES-128
        } catch (Exception e) {
            throw new IllegalStateException("Error al inicializar la clave criptográfica para SensitiveDataCryptoConverter", e);
        }
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isBlank()) {
            return null;
        }

        // Si ya está cifrado, evitar doble cifrado
        if (attribute.startsWith(PREFIX)) {
            return attribute;
        }

        try {
            SecretKeySpec secretKey = new SecretKeySpec(KEY_BYTES, ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            byte[] encrypted = cipher.doFinal(attribute.trim().getBytes(StandardCharsets.UTF_8));
            return PREFIX + Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("Error al cifrar campo sensible para la base de datos: {}", e.getMessage());
            // En caso de fallo inesperado, preservar dato para no perder transacción
            return attribute;
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }

        // Si no tiene el prefijo de cifrado, es un registro histórico en texto plano (seed data)
        if (!dbData.startsWith(PREFIX)) {
            return dbData;
        }

        try {
            String cipherText = dbData.substring(PREFIX.length());
            byte[] decoded = Base64.getDecoder().decode(cipherText);

            SecretKeySpec secretKey = new SecretKeySpec(KEY_BYTES, ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("No se pudo descifrar dato sensible (posible texto plano con prefijo): {}", e.getMessage());
            return dbData;
        }
    }
}
