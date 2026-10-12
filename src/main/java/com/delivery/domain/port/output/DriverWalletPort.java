package com.delivery.domain.port.output;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Puerto de Salida — Billetera y Liquidación de Saldo del Repartidor.
 *
 * <p>Responsable de gestionar la liberación de fondos retenidos (escrow)
 * hacia la cuenta o saldo del repartidor una vez confirmada la entrega exitosa
 * del pedido mediante PIN anti-fraude.</p>
 *
 * @since 1.0.0
 */
public interface DriverWalletPort {

    /**
     * Libera el saldo del pedido al repartidor asignado.
     *
     * @param orderId  identificador del pedido entregado
     * @param driverId identificador del repartidor receptor
     * @param amount   monto total o compensación a liquidar
     */
    void releaseEscrowFunds(UUID orderId, UUID driverId, BigDecimal amount);
}
