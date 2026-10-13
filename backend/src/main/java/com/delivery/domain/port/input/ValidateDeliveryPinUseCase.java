package com.delivery.domain.port.input;

import com.delivery.domain.port.input.command.ValidateDeliveryPinCommand;

/**
 * Puerto de Entrada — Caso de uso: Validar PIN de Entrega (Anti-Fraude).
 *
 * <p>Define el contrato para la verificación del PIN OTP de 4 dígitos
 * al momento de la entrega. Este es el componente central del flujo
 * anti-fraude del sistema.</p>
 *
 * <h3>Flujo de ejecución esperado:</h3>
 * <ol>
 *   <li>Recuperar el pedido por ID vía {@link com.delivery.domain.port.output.OrderRepositoryPort}</li>
 *   <li>Validar que el pedido esté en estado {@code ON_THE_WAY}</li>
 *   <li>Invocar {@link com.delivery.domain.model.Order#confirmDelivery(String)}</li>
 *   <li>Si el PIN es correcto → transicionar a {@code DELIVERED}</li>
 *   <li>Persistir el cambio de estado</li>
 *   <li>Notificar al cliente que la entrega fue confirmada</li>
 * </ol>
 *
 * <h3>Protecciones anti-fraude aplicadas:</h3>
 * <ul>
 *   <li>Máximo 5 intentos de verificación antes de bloqueo</li>
 *   <li>Comparación en tiempo constante (anti timing-attack)</li>
 *   <li>PIN con TTL de 2 horas (anti replay)</li>
 * </ul>
 *
 * <h3>Excepciones esperadas:</h3>
 * <ul>
 *   <li>{@link com.delivery.domain.exception.OrderNotFoundException} — pedido no existe</li>
 *   <li>{@link com.delivery.domain.exception.InvalidStateTransitionException} — pedido no está en ON_THE_WAY</li>
 *   <li>{@link com.delivery.domain.exception.InvalidDeliveryPinException} — PIN incorrecto, expirado o bloqueado</li>
 * </ul>
 *
 * @see ValidateDeliveryPinCommand
 * @since 1.0.0
 */
public interface ValidateDeliveryPinUseCase {

    /**
     * Verifica el PIN de entrega y, si es correcto, confirma la entrega
     * transicionando el pedido a {@code DELIVERED}.
     *
     * @param command contiene el ID del pedido y el PIN candidato
     * @return {@code true} si el PIN es correcto y la entrega fue confirmada
     */
    boolean execute(ValidateDeliveryPinCommand command);
}
