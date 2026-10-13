package com.delivery.infrastructure.adapter.output.payment;

import com.delivery.domain.model.DeliveryPin;
import com.delivery.domain.model.GeoLocation;
import com.delivery.domain.model.Order;
import com.delivery.domain.model.SettlementResult;
import com.delivery.domain.port.output.DriverWalletPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@DisplayName("Pruebas Unitarias — Módulo de Liquidación Financiera ($ USD)")
class FinancialSettlementAdapterTest {

    private DriverWalletPort driverWalletPort;
    private FinancialSettlementAdapter settlementAdapter;

    @BeforeEach
    void setUp() {
        driverWalletPort = Mockito.mock(DriverWalletPort.class);
        settlementAdapter = new FinancialSettlementAdapter(driverWalletPort);
    }

    @Test
    @DisplayName("Debe calcular liquidación: Comercio recibe subtotal menos comisión, Repartidor recibe 100% de envío")
    void shouldSettleOrderAccordingToRules() {
        UUID customerId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();

        // Total $12.50 USD (Subtotal $11.00 USD + Envío $1.50 USD)
        BigDecimal totalAmount = new BigDecimal("12.50");
        BigDecimal shippingFee = new BigDecimal("1.50");
        BigDecimal commissionRate = new BigDecimal("0.15"); // 15%

        Order order = Order.create(
                customerId,
                merchantId,
                "Av. Sucre y Bolívar, Tulcán",
                new GeoLocation(0.8116, -77.7172),
                "Calle Brasil, Tulcán",
                new GeoLocation(0.8035, -77.7245),
                totalAmount,
                "Pedido demo"
        );
        order.assignDriver(driverId);

        SettlementResult result = settlementAdapter.settleOrder(order, shippingFee, commissionRate);

        assertNotNull(result);
        assertEquals(new BigDecimal("12.50"), result.totalAmount());
        assertEquals(new BigDecimal("1.50"), result.shippingFee());
        assertEquals(new BigDecimal("11.00"), result.subtotal()); // 12.50 - 1.50

        // Comisión plataforma: 11.00 * 0.15 = $1.65 USD
        assertEquals(new BigDecimal("1.65"), result.platformCommission());

        // Comercio recibe: 11.00 - 1.65 = $9.35 USD
        assertEquals(new BigDecimal("9.35"), result.merchantPayout());

        // Repartidor recibe: 100% del costo de envío = $1.50 USD
        assertEquals(new BigDecimal("1.50"), result.driverPayout());

        // Verificar que se invocó la liberación al driver wallet
        verify(driverWalletPort).releaseEscrowFunds(any(), eq(driverId), eq(new BigDecimal("1.50")));
    }
}
