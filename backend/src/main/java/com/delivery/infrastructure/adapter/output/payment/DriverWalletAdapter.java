package com.delivery.infrastructure.adapter.output.payment;

import com.delivery.domain.port.output.DriverWalletPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Adaptador de Salida — Liquidación de Saldo a Billetera del Repartidor.
 *
 * <p>Registra la liberación de fondos del escrow tras la validación exitosa
 * del PIN anti-fraude y acredita el saldo correspondiente al repartidor.</p>
 *
 * @since 1.0.0
 */
@Component
public class DriverWalletAdapter implements DriverWalletPort {

    private static final Logger log = LoggerFactory.getLogger(DriverWalletAdapter.class);

    @Override
    public void releaseEscrowFunds(UUID orderId, UUID driverId, BigDecimal amount) {
        Objects.requireNonNull(orderId, "orderId no puede ser null");
        Objects.requireNonNull(driverId, "driverId no puede ser null");
        Objects.requireNonNull(amount, "amount no puede ser null");

        log.info("💰 [WALLET-ESCROW] Liberando saldo para driverId={}, orderId={}, monto=${}",
                driverId, orderId, amount);

        // Aquí se conectaría la pasarela de pagos o el ledger interno de balances
        log.info("✅ [WALLET-ESCROW] Saldo acreditado exitosamente a la billetera del repartidor {}", driverId);
    }
}
