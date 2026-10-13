package com.delivery.infrastructure.adapter.output.payment;

import com.delivery.domain.model.Order;
import com.delivery.domain.model.SettlementResult;
import com.delivery.domain.port.output.DriverWalletPort;
import com.delivery.domain.port.output.FinancialSettlementPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Adaptador de Salida — Módulo de Pagos y Liquidación Financiera.
 *
 * <p>Implementa la distribución de fondos post-entrega conforme a las reglas
 * de negocio de Tulcán, Ecuador en moneda $ USD:</p>
 * <ul>
 *   <li>Comercio: recibe el subtotal neto de venta menos la comisión de plataforma.</li>
 *   <li>Repartidor: recibe el 100% de la tarifa de envío urbano ($1.50 USD estándar).</li>
 *   <li>Plataforma BMCP: retiene la comisión pactada.</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Component
public class FinancialSettlementAdapter implements FinancialSettlementPort {

    private static final Logger log = LoggerFactory.getLogger(FinancialSettlementAdapter.class);

    private static final BigDecimal DEFAULT_SHIPPING_FEE = new BigDecimal("1.50");
    private static final BigDecimal DEFAULT_COMMISSION_RATE = new BigDecimal("0.15"); // 15%

    private final DriverWalletPort driverWalletPort;

    public FinancialSettlementAdapter(DriverWalletPort driverWalletPort) {
        this.driverWalletPort = driverWalletPort;
    }

    @Override
    public SettlementResult settleOrder(Order order, BigDecimal shippingFee, BigDecimal commissionRate) {
        Objects.requireNonNull(order, "order no puede ser null");

        BigDecimal total = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal fee = shippingFee != null ? shippingFee : DEFAULT_SHIPPING_FEE;
        BigDecimal rate = commissionRate != null ? commissionRate : DEFAULT_COMMISSION_RATE;

        // Subtotal = total - costo de envío (mínimo 0)
        BigDecimal subtotal = total.subtract(fee).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        // Comisión de la plataforma = subtotal * tasa
        BigDecimal platformCommission = subtotal.multiply(rate).setScale(2, RoundingMode.HALF_UP);

        // Pago neto al comercio = subtotal - comisión
        BigDecimal merchantPayout = subtotal.subtract(platformCommission).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        // Pago al repartidor = 100% del costo de envío
        BigDecimal driverPayout = fee.setScale(2, RoundingMode.HALF_UP);

        UUID orderId = order.getId() != null ? order.getId() : UUID.randomUUID();

        SettlementResult result = new SettlementResult(
                orderId,
                total,
                fee,
                subtotal,
                rate,
                platformCommission,
                merchantPayout,
                driverPayout,
                Instant.now()
        );

        log.info("💳 [FINANCIAL-SETTLEMENT] Liquidación procesada para orden {}: Total=${} USD, "
                        + "Subtotal=${} USD, Comisión Plataforma (${}%) = ${} USD, "
                        + "Liquidación Comercio=${} USD, Liquidación Repartidor (100% envío)=${} USD",
                order.getId(), total, subtotal, rate.multiply(BigDecimal.valueOf(100)),
                platformCommission, merchantPayout, driverPayout);

        // Acreditar billetera del repartidor si está asignado
        if (order.getDriverId() != null && driverWalletPort != null) {
            driverWalletPort.releaseEscrowFunds(order.getId(), order.getDriverId(), driverPayout);
        }

        return result;
    }
}
