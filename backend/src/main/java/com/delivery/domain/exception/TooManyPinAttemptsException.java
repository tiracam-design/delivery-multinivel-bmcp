package com.delivery.domain.exception;

import java.util.UUID;

/**
 * Excepción lanzada cuando se supera el umbral de intentos fallidos permitidos
 * para verificar el PIN OTP de entrega de un pedido (RULE-002, BMCP v1.0.0).
 *
 * <p>Protección contra ataques de fuerza bruta implementada mediante Redis
 * Rate Limiting con ventana de bloqueo temporal de 5 minutos.</p>
 *
 * @since 1.0.0
 */
public class TooManyPinAttemptsException extends DomainException {

    private static final String CODE = "ORDER.TOO_MANY_PIN_ATTEMPTS";

    private final UUID orderId;
    private final int attempts;

    /**
     * @param orderId  identificador único del pedido
     * @param attempts número de intentos fallidos alcanzados
     */
    public TooManyPinAttemptsException(UUID orderId, int attempts) {
        super(CODE, String.format(
                "Límite de intentos excedido (%d/3) para el pedido %s. "
                        + "Validación de PIN bloqueada por 5 minutos.",
                attempts, orderId));
        this.orderId = orderId;
        this.attempts = attempts;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public int getAttempts() {
        return attempts;
    }
}
