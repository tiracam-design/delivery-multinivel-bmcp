package com.delivery.domain.port.output;

import com.delivery.domain.model.Order;
import com.delivery.domain.model.SettlementResult;

import java.math.BigDecimal;

/**
 * Puerto de Salida — Módulo de Pagos y Liquidación Financiera.
 *
 * <p>Define el contrato para la distribución de fondos una vez validado
 * el PIN OTP de entrega:</p>
 * <ul>
 *   <li>El comercio recibe el subtotal menos la comisión de plataforma.</li>
 *   <li>El repartidor recibe el 100% del costo de envío.</li>
 * </ul>
 *
 * @since 1.0.0
 */
public interface FinancialSettlementPort {

    /**
     * Liquida los fondos de una orden entregada.
     *
     * @param order          orden entregada con PIN verificado
     * @param shippingFee    costo de envío fijado para la entrega
     * @param commissionRate tasa de comisión de plataforma (ej. 0.15 para 15%)
     * @return resultado detallado de la distribución financiera
     */
    SettlementResult settleOrder(Order order, BigDecimal shippingFee, BigDecimal commissionRate);
}
