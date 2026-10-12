package com.delivery.domain.exception;

import com.delivery.domain.enums.OrderStatus;

/**
 * Se lanza cuando se intenta una transición de estado no permitida
 * por la máquina de estados del pedido.
 *
 * <p>Cada {@link OrderStatus} define un conjunto finito de estados
 * sucesores válidos. Cualquier transición fuera de ese conjunto
 * constituye una violación a las reglas de negocio del flujo BMCP
 * y dispara esta excepción.</p>
 *
 * <h3>Ejemplo de uso:</h3>
 * <pre>{@code
 *   // Desde DELIVERED no se puede volver a ON_THE_WAY
 *   order.transitionTo(OrderStatus.ON_THE_WAY);
 *   // → lanza InvalidStateTransitionException
 * }</pre>
 *
 * @see OrderStatus#canTransitionTo(OrderStatus)
 * @since 1.0.0
 */
public class InvalidStateTransitionException extends DomainException {

    private static final String CODE = "ORDER.INVALID_TRANSITION";

    private final OrderStatus currentStatus;
    private final OrderStatus targetStatus;

    /**
     * @param currentStatus estado actual del pedido
     * @param targetStatus  estado al que se intentó transicionar
     */
    public InvalidStateTransitionException(OrderStatus currentStatus,
                                           OrderStatus targetStatus) {
        super(
                CODE,
                String.format(
                        "Transición de estado inválida: [%s] → [%s]. "
                                + "Transiciones permitidas desde [%s]: %s",
                        currentStatus,
                        targetStatus,
                        currentStatus,
                        currentStatus.getAllowedTransitions()
                )
        );
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }

    /** @return estado en el que se encontraba el pedido */
    public OrderStatus getCurrentStatus() {
        return currentStatus;
    }

    /** @return estado al que se intentó transicionar */
    public OrderStatus getTargetStatus() {
        return targetStatus;
    }
}
