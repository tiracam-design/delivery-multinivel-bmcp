package com.delivery.domain.model;

import com.delivery.domain.enums.OrderStatus;
import com.delivery.domain.exception.InvalidDeliveryPinException;
import com.delivery.domain.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para la entidad {@link Order}.
 *
 * <p>Valida la máquina de estados, el flujo anti-fraude con PIN OTP,
 * y las invariantes de negocio del agregado de pedidos.</p>
 */
@DisplayName("Order — Entidad de Dominio")
class OrderTest {

    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID MERCHANT_ID = UUID.randomUUID();
    private static final UUID DRIVER_UUID = UUID.randomUUID();

    private Order order;

    @BeforeEach
    void setUp() {
        order = Order.create(
                CUSTOMER_ID,
                MERCHANT_ID,
                "Calle Sucre 14-20 y Pichincha, Parque Central, Tulcán",
                new GeoLocation(0.8116, -77.7172),
                "Av. Coral 450 y Av. Manabí, Terminal Terrestre, Tulcán",
                new GeoLocation(0.8035, -77.7245),
                new BigDecimal("12.50"),
                "Sin cebolla"
        );
    }

    // ════════════════════════════════════════════════════════════════
    //  CREACIÓN
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Creación de pedidos")
    class Creation {

        @Test
        @DisplayName("Debe crear pedido en estado CREATED con PIN generado")
        void shouldCreateOrderWithCorrectDefaults() {
            assertAll("estado inicial",
                    () -> assertEquals(OrderStatus.CREATED, order.getStatus()),
                    () -> assertNotNull(order.getDeliveryPin()),
                    () -> assertNull(order.getId()),
                    () -> assertNull(order.getDriverId()),
                    () -> assertEquals(CUSTOMER_ID, order.getCustomerId()),
                    () -> assertNotNull(order.getCreatedAt()),
                    () -> assertNotNull(order.getUpdatedAt()),
                    () -> assertEquals(1, order.getStatusHistory().size())
            );
        }

        @Test
        @DisplayName("Debe calcular distancia estimada si hay geolocalizaciones")
        void shouldCalculateEstimatedDistance() {
            assertNotNull(order.getEstimatedDistanceKm());
            assertTrue(order.getEstimatedDistanceKm() > 0);
        }

        @Test
        @DisplayName("Debe fallar si customerId es null")
        void shouldRejectNullCustomerId() {
            assertThrows(NullPointerException.class, () ->
                    Order.create(null, MERCHANT_ID, "Dir A", null, "Dir B", null,
                            BigDecimal.TEN, null));
        }

        @Test
        @DisplayName("Debe fallar si totalAmount es cero o negativo")
        void shouldRejectInvalidAmount() {
            assertThrows(IllegalArgumentException.class, () ->
                    Order.create(CUSTOMER_ID, MERCHANT_ID, "Dir A", null, "Dir B", null,
                            BigDecimal.ZERO, null));
            assertThrows(IllegalArgumentException.class, () ->
                    Order.create(CUSTOMER_ID, MERCHANT_ID, "Dir A", null, "Dir B", null,
                            new BigDecimal("-10"), null));
        }

        @Test
        @DisplayName("Debe fallar si pickupAddress está vacío")
        void shouldRejectBlankPickupAddress() {
            assertThrows(IllegalArgumentException.class, () ->
                    Order.create(CUSTOMER_ID, MERCHANT_ID, "  ", null, "Dir B", null,
                            BigDecimal.TEN, null));
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  MÁQUINA DE ESTADOS — Flujo feliz
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Máquina de estados — Flujo feliz")
    class HappyPath {

        @Test
        @DisplayName("Debe completar el flujo completo: CREATED → ... → DELIVERED")
        void shouldCompleteFullLifecycle() {
            // CREATED → ACCEPTED
            order.transitionTo(OrderStatus.ACCEPTED, "Comercio aceptó");
            assertEquals(OrderStatus.ACCEPTED, order.getStatus());

            // ACCEPTED → IN_PREPARATION
            order.transitionTo(OrderStatus.IN_PREPARATION);
            assertEquals(OrderStatus.IN_PREPARATION, order.getStatus());

            // IN_PREPARATION → READY_FOR_PICKUP
            order.transitionTo(OrderStatus.READY_FOR_PICKUP);
            assertEquals(OrderStatus.READY_FOR_PICKUP, order.getStatus());

            // READY_FOR_PICKUP → ON_THE_WAY
            order.transitionTo(OrderStatus.ON_THE_WAY);
            assertEquals(OrderStatus.ON_THE_WAY, order.getStatus());

            // ON_THE_WAY → DELIVERED (requiere PIN)
            String pin = order.getDeliveryPin().getValue();
            order.confirmDelivery(pin);
            assertEquals(OrderStatus.DELIVERED, order.getStatus());

            // Historial completo
            assertEquals(6, order.getStatusHistory().size());
            assertTrue(order.isDelivered());
            assertFalse(order.isActive());
        }

        @Test
        @DisplayName("Debe registrar historial con razones")
        void shouldRecordStatusHistory() {
            order.transitionTo(OrderStatus.ACCEPTED, "Comercio verificado");

            var history = order.getStatusHistory();
            assertEquals(2, history.size());

            var lastChange = history.get(1);
            assertEquals(OrderStatus.CREATED, lastChange.getFromStatus());
            assertEquals(OrderStatus.ACCEPTED, lastChange.getToStatus());
            assertEquals("Comercio verificado", lastChange.getReason());
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  MÁQUINA DE ESTADOS — Transiciones inválidas
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Máquina de estados — Transiciones inválidas")
    class InvalidTransitions {

        @Test
        @DisplayName("No debe permitir saltar estados (CREATED → ON_THE_WAY)")
        void shouldRejectSkippingStates() {
            var ex = assertThrows(InvalidStateTransitionException.class,
                    () -> order.transitionTo(OrderStatus.ON_THE_WAY));

            assertEquals(OrderStatus.CREATED, ex.getCurrentStatus());
            assertEquals(OrderStatus.ON_THE_WAY, ex.getTargetStatus());
        }

        @Test
        @DisplayName("No debe permitir retroceder (ACCEPTED → CREATED)")
        void shouldRejectGoingBackwards() {
            order.transitionTo(OrderStatus.ACCEPTED);
            assertThrows(InvalidStateTransitionException.class,
                    () -> order.transitionTo(OrderStatus.CREATED));
        }

        @Test
        @DisplayName("No debe permitir transiciones desde DELIVERED")
        void shouldRejectTransitionsFromDelivered() {
            advanceToOnTheWay();
            order.confirmDelivery(order.getDeliveryPin().getValue());

            assertThrows(InvalidStateTransitionException.class,
                    () -> order.transitionTo(OrderStatus.ON_THE_WAY));
        }

        @Test
        @DisplayName("No debe permitir transiciones desde CANCELLED")
        void shouldRejectTransitionsFromCancelled() {
            order.transitionTo(OrderStatus.CANCELLED);

            assertThrows(InvalidStateTransitionException.class,
                    () -> order.transitionTo(OrderStatus.ACCEPTED));
        }

        @Test
        @DisplayName("No debe permitir DELIVERED sin PIN (via transitionTo)")
        void shouldRejectDeliveredWithoutPin() {
            advanceToOnTheWay();

            assertThrows(InvalidStateTransitionException.class,
                    () -> order.transitionTo(OrderStatus.DELIVERED));
        }

        @ParameterizedTest
        @EnumSource(value = OrderStatus.class,
                names = {"IN_PREPARATION", "READY_FOR_PICKUP", "DELIVERED",
                         "INCIDENT_PAUSED"})
        @DisplayName("CREATED solo puede ir a ACCEPTED o CANCELLED")
        void shouldRejectInvalidTransitionsFromCreated(OrderStatus invalid) {
            assertThrows(InvalidStateTransitionException.class,
                    () -> order.transitionTo(invalid));
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  FLUJO ANTI-FRAUDE — PIN de entrega
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Anti-fraude — PIN de entrega")
    class DeliveryPinValidation {

        @BeforeEach
        void advanceToDelivery() {
            advanceToOnTheWay();
        }

        @Test
        @DisplayName("Debe confirmar entrega con PIN correcto")
        void shouldConfirmWithCorrectPin() {
            String pin = order.getDeliveryPin().getValue();

            assertDoesNotThrow(() -> order.confirmDelivery(pin));
            assertEquals(OrderStatus.DELIVERED, order.getStatus());
        }

        @Test
        @DisplayName("Debe rechazar PIN incorrecto")
        void shouldRejectIncorrectPin() {
            assertThrows(InvalidDeliveryPinException.class,
                    () -> order.confirmDelivery("0000"));
        }

        @Test
        @DisplayName("Debe rechazar PIN con formato inválido")
        void shouldRejectInvalidFormat() {
            assertThrows(InvalidDeliveryPinException.class,
                    () -> order.confirmDelivery("ABC"));
            assertThrows(InvalidDeliveryPinException.class,
                    () -> order.confirmDelivery("12345"));
            assertThrows(InvalidDeliveryPinException.class,
                    () -> order.confirmDelivery(null));
        }

        @Test
        @DisplayName("Debe bloquear tras 5 intentos fallidos")
        void shouldBlockAfterMaxAttempts() {
            for (int i = 0; i < DeliveryPin.MAX_VERIFICATION_ATTEMPTS; i++) {
                try {
                    order.confirmDelivery("9999");
                } catch (InvalidDeliveryPinException ignored) {
                    // esperado
                }
            }

            // El 6to intento debe fallar por bloqueo
            var ex = assertThrows(InvalidDeliveryPinException.class,
                    () -> order.confirmDelivery(
                            order.getDeliveryPin().getValue()));

            assertTrue(ex.getMessage().contains("bloqueado"));
            assertTrue(order.getDeliveryPin().isBlocked());
        }

        @Test
        @DisplayName("Debe permitir regenerar PIN bloqueado")
        void shouldAllowPinRegeneration() {
            order.regenerateDeliveryPin();
            String newPin = order.getDeliveryPin().getValue();

            order.confirmDelivery(newPin);
            assertEquals(OrderStatus.DELIVERED, order.getStatus());
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  FLUJO DE INCIDENTES
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Flujo de incidentes")
    class IncidentFlow {

        @Test
        @DisplayName("Debe pausar por incidente y retomar ruta")
        void shouldPauseAndResume() {
            advanceToOnTheWay();

            order.transitionTo(OrderStatus.INCIDENT_PAUSED, "Dirección incorrecta");
            assertEquals(OrderStatus.INCIDENT_PAUSED, order.getStatus());

            order.transitionTo(OrderStatus.ON_THE_WAY, "Dirección corregida");
            assertEquals(OrderStatus.ON_THE_WAY, order.getStatus());

            // Confirmar entrega después de retomar
            order.confirmDelivery(order.getDeliveryPin().getValue());
            assertTrue(order.isDelivered());
        }

        @Test
        @DisplayName("Debe permitir cancelar desde incidente")
        void shouldAllowCancelFromIncident() {
            advanceToOnTheWay();
            order.transitionTo(OrderStatus.INCIDENT_PAUSED);

            order.transitionTo(OrderStatus.CANCELLED, "Cliente canceló");
            assertTrue(order.isCancelled());
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  CANCELACIÓN
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Cancelación")
    class Cancellation {

        @ParameterizedTest
        @EnumSource(value = OrderStatus.class,
                names = {"CREATED", "ACCEPTED", "IN_PREPARATION",
                         "READY_FOR_PICKUP"})
        @DisplayName("Debe permitir cancelar desde estados previos a ON_THE_WAY")
        void shouldAllowCancellationFromEarlyStates(OrderStatus state) {
            advanceTo(state);
            assertTrue(order.isCancellable());
            order.transitionTo(OrderStatus.CANCELLED);
            assertTrue(order.isCancelled());
        }

        @Test
        @DisplayName("No debe permitir cancelar ON_THE_WAY directamente")
        void shouldNotCancelFromOnTheWay() {
            advanceToOnTheWay();
            assertFalse(order.isCancellable());
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  ASIGNACIÓN DE REPARTIDOR
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Asignación de repartidor")
    class DriverAssignment {

        @Test
        @DisplayName("Debe asignar repartidor a pedido activo")
        void shouldAssignDriver() {
            order.assignDriver(DRIVER_UUID);
            assertEquals(DRIVER_UUID, order.getDriverId());
            assertTrue(order.hasDriverAssigned());
        }

        @Test
        @DisplayName("No debe asignar repartidor a pedido terminal")
        void shouldRejectAssignmentOnTerminal() {
            order.transitionTo(OrderStatus.CANCELLED);
            assertThrows(IllegalStateException.class,
                    () -> order.assignDriver(DRIVER_UUID));
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private void advanceToOnTheWay() {
        order.transitionTo(OrderStatus.ACCEPTED);
        order.transitionTo(OrderStatus.IN_PREPARATION);
        order.transitionTo(OrderStatus.READY_FOR_PICKUP);
        order.transitionTo(OrderStatus.ON_THE_WAY);
    }

    private void advanceTo(OrderStatus target) {
        OrderStatus[] path = {
                OrderStatus.ACCEPTED,
                OrderStatus.IN_PREPARATION,
                OrderStatus.READY_FOR_PICKUP,
                OrderStatus.ON_THE_WAY
        };
        for (OrderStatus step : path) {
            if (order.getStatus() == target) break;
            order.transitionTo(step);
        }
    }
}
