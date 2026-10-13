package com.delivery.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Registro de liquidación financiera del pedido entregado.
 *
 * <p>Aplica la regla de negocio del sistema de delivery:
 * <ul>
 *   <li><strong>Comercio:</strong> recibe el subtotal de productos menos la comisión de plataforma.</li>
 *   <li><strong>Repartidor:</strong> recibe el 100% del costo de envío urbano.</li>
 *   <li><strong>Plataforma BMCP:</strong> retiene el porcentaje de comisión acordado.</li>
 * </ul>
 * </p>
 *
 * @since 1.0.0
 */
public record SettlementResult(
        UUID orderId,
        BigDecimal totalAmount,
        BigDecimal shippingFee,
        BigDecimal subtotal,
        BigDecimal commissionRate,
        BigDecimal platformCommission,
        BigDecimal merchantPayout,
        BigDecimal driverPayout,
        Instant settledAt
) {
    public SettlementResult {
        Objects.requireNonNull(orderId, "orderId es obligatorio");
        Objects.requireNonNull(totalAmount, "totalAmount es obligatorio");
        Objects.requireNonNull(shippingFee, "shippingFee es obligatorio");
        Objects.requireNonNull(subtotal, "subtotal es obligatorio");
        Objects.requireNonNull(merchantPayout, "merchantPayout es obligatorio");
        Objects.requireNonNull(driverPayout, "driverPayout es obligatorio");
        if (settledAt == null) {
            settledAt = Instant.now();
        }
    }
}
