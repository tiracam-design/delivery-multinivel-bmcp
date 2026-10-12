package com.delivery.domain.exception;

import java.util.UUID;

/**
 * Se lanza cuando se intenta buscar o manipular un pedido que
 * no existe en el sistema.
 *
 * @since 1.0.0
 */
public class OrderNotFoundException extends DomainException {

    private static final String CODE = "ORDER.NOT_FOUND";

    public OrderNotFoundException(UUID orderId) {
        super(CODE, String.format("Pedido con ID [%s] no encontrado", orderId));
    }
}
