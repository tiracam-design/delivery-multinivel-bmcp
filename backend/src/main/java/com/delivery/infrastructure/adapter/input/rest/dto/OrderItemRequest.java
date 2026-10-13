package com.delivery.infrastructure.adapter.input.rest.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Detalle de un producto / ítem de menú dentro de la solicitud de pedido.
 *
 * @since 1.0.0
 */
public record OrderItemRequest(
        UUID productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice
) {}
