package com.delivery.domain.port.output;

import com.delivery.domain.exception.TooManyPinAttemptsException;

import java.util.UUID;

/**
 * Puerto de Salida — Control de Frecuencia (Rate Limiting) para PIN OTP.
 *
 * <p>Define el contrato para registrar, verificar y resetear los intentos
 * fallidos de validación de PIN en almacenamiento volátil de alta velocidad (Redis).</p>
 *
 * @since 1.0.0
 */
public interface PinRateLimiterPort {

    /**
     * Verifica si el pedido tiene permitido intentar validar el PIN.
     *
     * @param orderId identificador del pedido
     * @throws TooManyPinAttemptsException si el pedido ya alcanzó 3 intentos fallidos
     */
    void checkPinAttemptAllowed(UUID orderId);

    /**
     * Incrementa el contador de intentos fallidos en Redis con TTL de 5 minutos.
     * Si el contador alcanza 3 intentos, lanza {@link TooManyPinAttemptsException}.
     *
     * @param orderId identificador del pedido
     * @throws TooManyPinAttemptsException si se alcanza el umbral de 3 intentos
     */
    void recordFailedAttempt(UUID orderId);

    /**
     * Resetea el contador de intentos al validar exitosamente el PIN.
     *
     * @param orderId identificador del pedido
     */
    void resetAttempts(UUID orderId);

    /**
     * Retorna el número de intentos restantes antes de ser bloqueado.
     *
     * @param orderId identificador del pedido
     * @return intentos restantes (entre 0 y 3)
     */
    int getRemainingAttempts(UUID orderId);
}
