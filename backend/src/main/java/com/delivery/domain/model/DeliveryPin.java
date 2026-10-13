package com.delivery.domain.model;

import com.delivery.domain.exception.InvalidDeliveryPinException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Objects;

/**
 * Value Object inmutable que encapsula el PIN de entrega (código OTP
 * de 4 dígitos) usado como mecanismo anti-fraude.
 *
 * <h3>Flujo anti-fraude:</h3>
 * <ol>
 *   <li>Al crear el pedido, se genera un {@code DeliveryPin} aleatorio.</li>
 *   <li>El PIN se envía al <strong>cliente</strong> (SMS, push, email).</li>
 *   <li>Al llegar al destino, el <strong>repartidor</strong> solicita el PIN.</li>
 *   <li>El sistema valida el PIN antes de confirmar la entrega.</li>
 *   <li>Si el PIN no coincide → se bloquea la transición a {@code DELIVERED}.</li>
 * </ol>
 *
 * <h3>Reglas de validación:</h3>
 * <ul>
 *   <li>Exactamente 4 dígitos numéricos ({@code 0000}–{@code 9999})</li>
 *   <li>Generado con {@link SecureRandom} (criptográficamente seguro)</li>
 *   <li>Tiene un tiempo de expiración configurable</li>
 *   <li>Solo puede verificarse un número limitado de veces (anti brute-force)</li>
 * </ul>
 *
 * <p>Al ser un <strong>Value Object</strong>, la igualdad se basa en el
 * valor del PIN, no en identidad de referencia.</p>
 *
 * @since 1.0.0
 */
public final class DeliveryPin {

    /** Longitud exacta del PIN */
    public static final int PIN_LENGTH = 4;

    /** Máximo de intentos de verificación antes de bloquear */
    public static final int MAX_VERIFICATION_ATTEMPTS = 5;

    /** Tiempo de vida del PIN en segundos (por defecto 2 horas) */
    private static final long DEFAULT_TTL_SECONDS = 7_200L;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final String value;
    private final Instant createdAt;
    private final Instant expiresAt;
    private int verificationAttempts;

    // ── Constructores ───────────────────────────────────────────────

    /**
     * Crea un PIN con un valor específico (para reconstrucción desde
     * persistencia).
     *
     * @param value     cadena de 4 dígitos
     * @param createdAt momento de creación
     * @param expiresAt momento de expiración
     * @param attempts  intentos de verificación consumidos
     * @throws InvalidDeliveryPinException si el formato es inválido
     */
    public DeliveryPin(String value, Instant createdAt, Instant expiresAt,
                       int attempts) {
        validateFormat(value);
        this.value = value;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt no puede ser null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt no puede ser null");
        this.verificationAttempts = attempts;
    }

    // ── Factory Methods ─────────────────────────────────────────────

    /**
     * Genera un nuevo PIN aleatorio criptográficamente seguro.
     *
     * @return nuevo {@code DeliveryPin} con TTL por defecto (2 horas)
     */
    public static DeliveryPin generate() {
        return generate(DEFAULT_TTL_SECONDS);
    }

    /**
     * Genera un nuevo PIN aleatorio con TTL personalizado.
     *
     * @param ttlSeconds tiempo de vida del PIN en segundos
     * @return nuevo {@code DeliveryPin}
     */
    public static DeliveryPin generate(long ttlSeconds) {
        String pin = String.format("%04d", SECURE_RANDOM.nextInt(10_000));
        Instant now = Instant.now();
        return new DeliveryPin(pin, now, now.plusSeconds(ttlSeconds), 0);
    }

    // ── Validación ──────────────────────────────────────────────────

    /**
     * Valida que el valor tenga exactamente 4 dígitos numéricos.
     *
     * @param value cadena a validar
     * @throws InvalidDeliveryPinException si el formato es incorrecto
     */
    private static void validateFormat(String value) {
        if (value == null || value.length() != PIN_LENGTH) {
            throw new InvalidDeliveryPinException(
                    String.format("Debe tener exactamente %d dígitos, recibido: '%s'",
                            PIN_LENGTH, value)
            );
        }
        for (char c : value.toCharArray()) {
            if (!Character.isDigit(c)) {
                throw new InvalidDeliveryPinException(
                        "Solo se permiten dígitos numéricos (0-9), recibido: '" + value + "'"
                );
            }
        }
    }

    // ── Verificación anti-fraude ────────────────────────────────────

    /**
     * Verifica si el PIN candidato coincide con este PIN.
     *
     * <p>Aplica las siguientes validaciones en orden:</p>
     * <ol>
     *   <li>¿Se excedió el máximo de intentos? → Bloqueo</li>
     *   <li>¿El PIN expiró? → Rechazo</li>
     *   <li>¿El formato es válido? → Validación</li>
     *   <li>¿Coincide el valor? → Comparación en tiempo constante</li>
     * </ol>
     *
     * @param candidatePin PIN proporcionado por el repartidor
     * @return {@code true} si el PIN es correcto
     * @throws InvalidDeliveryPinException si se excedieron los intentos,
     *         el PIN expiró, o el formato es inválido
     */
    public boolean verify(String candidatePin) {
        // Anti brute-force: limitar intentos
        if (verificationAttempts >= MAX_VERIFICATION_ATTEMPTS) {
            throw new InvalidDeliveryPinException(
                    String.format("Máximo de intentos de verificación excedido (%d/%d). "
                            + "PIN bloqueado por seguridad.",
                            verificationAttempts, MAX_VERIFICATION_ATTEMPTS)
            );
        }

        // Verificar expiración
        if (isExpired()) {
            throw new InvalidDeliveryPinException(
                    "El PIN ha expirado. Solicite un nuevo PIN al sistema."
            );
        }

        // Validar formato del candidato
        validateFormat(candidatePin);

        // Incrementar intentos ANTES de comparar (previene timing attacks
        // donde el atacante detecta si se llegó al final del método)
        verificationAttempts++;

        // Comparación en tiempo constante para prevenir timing attacks
        return constantTimeEquals(this.value, candidatePin);
    }

    /**
     * Comparación de strings en tiempo constante.
     *
     * <p>Evita ataques de temporización donde un atacante podría
     * inferir dígitos correctos midiendo el tiempo de respuesta.</p>
     *
     * @param a primera cadena
     * @param b segunda cadena
     * @return {@code true} si son idénticas
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    // ── Consultas ───────────────────────────────────────────────────

    /**
     * @return {@code true} si el PIN ha superado su tiempo de vida
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * @return {@code true} si se alcanzó el máximo de intentos de verificación
     */
    public boolean isBlocked() {
        return verificationAttempts >= MAX_VERIFICATION_ATTEMPTS;
    }

    /**
     * @return intentos de verificación restantes
     */
    public int getRemainingAttempts() {
        return Math.max(0, MAX_VERIFICATION_ATTEMPTS - verificationAttempts);
    }

    // ── Getters ─────────────────────────────────────────────────────

    /** @return valor del PIN (4 dígitos) — sensible, no loguear en producción */
    public String getValue() {
        return value;
    }

    /** @return momento de creación del PIN */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /** @return momento de expiración del PIN */
    public Instant getExpiresAt() {
        return expiresAt;
    }

    /** @return cantidad de intentos de verificación realizados */
    public int getVerificationAttempts() {
        return verificationAttempts;
    }

    // ── Value Object: equals & hashCode por valor ───────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeliveryPin that)) return false;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    /**
     * Representación segura que enmascara el PIN para logs.
     *
     * @return {@code "DeliveryPin[****]"} (el valor real nunca se expone)
     */
    @Override
    public String toString() {
        return "DeliveryPin[****]";
    }
}
