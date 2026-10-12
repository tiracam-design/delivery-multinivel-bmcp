package com.delivery.application.service;

import com.delivery.domain.enums.NotificationType;
import com.delivery.domain.enums.OrderStatus;
import com.delivery.domain.exception.InvalidDeliveryPinException;
import com.delivery.domain.exception.InvalidStateTransitionException;
import com.delivery.domain.exception.OrderNotFoundException;
import com.delivery.domain.model.DeliveryPin;
import com.delivery.domain.model.GeoLocation;
import com.delivery.domain.model.Order;
import com.delivery.domain.port.input.command.ValidateDeliveryPinCommand;
import com.delivery.domain.port.output.NotificationPort;
import com.delivery.domain.port.output.OrderRepositoryPort;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para {@link ValidateDeliveryPinService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ValidateDeliveryPinService — Caso de Uso")
class ValidateDeliveryPinServiceTest {

    @Mock
    private OrderRepositoryPort orderRepository;

    @Mock
    private NotificationPort notificationPort;

    @InjectMocks
    private ValidateDeliveryPinService service;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    @Captor
    private ArgumentCaptor<Map<String, Object>> payloadCaptor;

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID MERCHANT_ID = UUID.randomUUID();
    private static final UUID DRIVER_ID = UUID.randomUUID();

    /**
     * Crea un pedido en estado ON_THE_WAY listo para confirmar entrega.
     */
    private Order createOrderOnTheWay() {
        Order order = Order.create(
                CUSTOMER_ID,
                MERCHANT_ID,
                "Calle Sucre 14-20 y Pichincha, Parque Central, Tulcán",
                new GeoLocation(0.8116, -77.7172),
                "Av. Coral 450 y Av. Manabí, Terminal Terrestre, Tulcán",
                new GeoLocation(0.8035, -77.7245),
                new BigDecimal("12.50"),
                "Tocar timbre"
        );

        order.transitionTo(OrderStatus.ACCEPTED, "Comercio aceptó");
        order.transitionTo(OrderStatus.IN_PREPARATION, "Preparando");
        order.transitionTo(OrderStatus.READY_FOR_PICKUP, "Listo");
        order.assignDriver(DRIVER_ID);
        order.transitionTo(OrderStatus.ON_THE_WAY, "Repartidor recogió");

        return order;
    }

    /**
     * Crea un pedido con ID simulando reconstrucción desde persistencia.
     */
    private Order createPersistedOrderOnTheWay() {
        Order order = createOrderOnTheWay();
        return new Order(
                ORDER_ID,
                order.getCustomerId(),
                order.getMerchantId(),
                order.getDriverId(),
                order.getStatus(),
                order.getPickupAddress(),
                order.getPickupLocation(),
                order.getDeliveryAddress(),
                order.getDeliveryLocation(),
                order.getEstimatedDistanceKm(),
                order.getEstimatedDurationMinutes(),
                order.getTotalAmount(),
                order.getDeliveryPin(),
                order.getNotes(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getStatusHistory()
        );
    }

    // ═══════════════════════════════════════════════════════════════
    //  ✅ ESCENARIO EXITOSO
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("✅ PIN correcto — Flujo exitoso")
    class SuccessScenario {

        @Test
        @DisplayName("Debe confirmar entrega, persistir y notificar")
        void shouldConfirmDeliveryWithCorrectPin() {
            Order order = createPersistedOrderOnTheWay();
            String correctPin = order.getDeliveryPin().getValue();
            var command = new ValidateDeliveryPinCommand(ORDER_ID, correctPin);

            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            boolean result = service.execute(command);

            assertTrue(result);
            verify(orderRepository).save(orderCaptor.capture());
            assertEquals(OrderStatus.DELIVERED, orderCaptor.getValue().getStatus());
        }

        @Test
        @DisplayName("Debe notificar al cliente con DELIVERY_CONFIRMED y payload correcto")
        void shouldNotifyCustomerOnSuccess() {
            Order order = createPersistedOrderOnTheWay();
            String correctPin = order.getDeliveryPin().getValue();
            var command = new ValidateDeliveryPinCommand(ORDER_ID, correctPin);

            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            service.execute(command);

            verify(notificationPort).notifyCustomer(
                    eq(CUSTOMER_ID), eq(NotificationType.DELIVERY_CONFIRMED),
                    payloadCaptor.capture());

            Map<String, Object> payload = payloadCaptor.getValue();
            assertEquals(ORDER_ID, payload.get("orderId"));
            assertEquals("DELIVERED", payload.get("status"));
            assertEquals(new BigDecimal("250.00"), payload.get("totalAmount"));
            assertNotNull(payload.get("deliveredAt"));
        }

        @Test
        @DisplayName("Debe notificar al repartidor con DELIVERY_CONFIRMED")
        void shouldNotifyDriverOnSuccess() {
            Order order = createPersistedOrderOnTheWay();
            var command = new ValidateDeliveryPinCommand(ORDER_ID,
                    order.getDeliveryPin().getValue());

            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            service.execute(command);

            verify(notificationPort).notifyDriver(
                    eq(DRIVER_ID), eq(NotificationType.DELIVERY_CONFIRMED), any());
        }

        @Test
        @DisplayName("Debe persistir exactamente 1 vez en éxito")
        void shouldSaveExactlyOnceOnSuccess() {
            Order order = createPersistedOrderOnTheWay();
            var command = new ValidateDeliveryPinCommand(ORDER_ID,
                    order.getDeliveryPin().getValue());

            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            service.execute(command);

            verify(orderRepository, times(1)).save(any(Order.class));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  ❌ PEDIDO NO ENCONTRADO
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("❌ Pedido no encontrado")
    class OrderNotFound {

        @Test
        @DisplayName("Debe lanzar OrderNotFoundException")
        void shouldThrowWhenOrderNotFound() {
            UUID missingId = UUID.randomUUID();
            var command = new ValidateDeliveryPinCommand(missingId, "1234");
            when(orderRepository.findById(missingId)).thenReturn(Optional.empty());

            assertThrows(OrderNotFoundException.class, () -> service.execute(command));

            verify(orderRepository, never()).save(any());
            verifyNoInteractions(notificationPort);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  ❌ ESTADO INVÁLIDO
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("❌ Estado inválido (no ON_THE_WAY)")
    class InvalidState {

        @Test
        @DisplayName("Debe lanzar excepción si el pedido está en CREATED")
        void shouldRejectWhenCreated() {
            assertRejectsState(OrderStatus.CREATED);
        }

        @Test
        @DisplayName("Debe lanzar excepción si el pedido ya fue DELIVERED")
        void shouldRejectWhenDelivered() {
            Order order = createPersistedOrderOnTheWay();
            order.confirmDelivery(order.getDeliveryPin().getValue());

            var command = new ValidateDeliveryPinCommand(ORDER_ID, "1234");
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

            assertThrows(InvalidStateTransitionException.class,
                    () -> service.execute(command));

            verify(orderRepository, never()).save(any());
            verifyNoInteractions(notificationPort);
        }

        @Test
        @DisplayName("Debe lanzar excepción si el pedido está CANCELLED")
        void shouldRejectWhenCancelled() {
            assertRejectsState(OrderStatus.CANCELLED);
        }

        private void assertRejectsState(OrderStatus state) {
            Order order = createOrderInState(state);
            var command = new ValidateDeliveryPinCommand(ORDER_ID, "1234");

            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

            assertThrows(InvalidStateTransitionException.class,
                    () -> service.execute(command));

            verify(orderRepository, never()).save(any());
            verifyNoInteractions(notificationPort);
        }

        private Order createOrderInState(OrderStatus targetStatus) {
            return new Order(
                    ORDER_ID, CUSTOMER_ID, MERCHANT_ID, DRIVER_ID,
                    targetStatus,
                    "Pickup", null, "Delivery", null,
                    null, null,
                    new BigDecimal("100.00"),
                    DeliveryPin.generate(),
                    null,
                    java.time.Instant.now(), java.time.Instant.now(),
                    new ArrayList<>()
            );
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  ❌ PIN INCORRECTO — Rate Limiting
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("❌ PIN incorrecto — Rate Limiting")
    class IncorrectPin {

        @Test
        @DisplayName("Debe lanzar InvalidDeliveryPinException con PIN incorrecto")
        void shouldThrowOnIncorrectPin() {
            Order order = createPersistedOrderOnTheWay();
            String wrongPin = order.getDeliveryPin().getValue().equals("0000")
                    ? "1111" : "0000";
            var command = new ValidateDeliveryPinCommand(ORDER_ID, wrongPin);

            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

            assertThrows(InvalidDeliveryPinException.class,
                    () -> service.execute(command));
        }

        @Test
        @DisplayName("Debe persistir intento fallido para rate limiting")
        void shouldSaveAfterFailedAttempt() {
            Order order = createPersistedOrderOnTheWay();
            String wrongPin = order.getDeliveryPin().getValue().equals("0000")
                    ? "1111" : "0000";
            var command = new ValidateDeliveryPinCommand(ORDER_ID, wrongPin);

            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

            assertThrows(InvalidDeliveryPinException.class,
                    () -> service.execute(command));

            verify(orderRepository).save(orderCaptor.capture());
            assertEquals(OrderStatus.ON_THE_WAY, orderCaptor.getValue().getStatus());
            assertEquals(1, orderCaptor.getValue().getDeliveryPin().getVerificationAttempts());
        }

        @Test
        @DisplayName("NO debe enviar notificaciones en intento fallido")
        void shouldNotNotifyOnFailure() {
            Order order = createPersistedOrderOnTheWay();
            String wrongPin = order.getDeliveryPin().getValue().equals("0000")
                    ? "1111" : "0000";
            var command = new ValidateDeliveryPinCommand(ORDER_ID, wrongPin);

            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

            assertThrows(InvalidDeliveryPinException.class,
                    () -> service.execute(command));

            verifyNoInteractions(notificationPort);
        }

        @Test
        @DisplayName("Debe bloquear PIN tras 5 intentos fallidos")
        void shouldBlockPinAfterMaxAttempts() {
            Order order = createPersistedOrderOnTheWay();
            String wrongPin = order.getDeliveryPin().getValue().equals("0000")
                    ? "1111" : "0000";

            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

            for (int i = 0; i < DeliveryPin.MAX_VERIFICATION_ATTEMPTS; i++) {
                var cmd = new ValidateDeliveryPinCommand(ORDER_ID, wrongPin);
                assertThrows(InvalidDeliveryPinException.class,
                        () -> service.execute(cmd));
            }

            // 6to intento con PIN correcto también falla (bloqueado)
            String correctPin = order.getDeliveryPin().getValue();
            var cmdCorrect = new ValidateDeliveryPinCommand(ORDER_ID, correctPin);
            assertThrows(InvalidDeliveryPinException.class,
                    () -> service.execute(cmdCorrect));

            assertTrue(order.getDeliveryPin().isBlocked());
            verify(orderRepository, times(6)).save(any(Order.class));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  🔒 VALIDACIONES DE BORDE
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("🔒 Validaciones de borde")
    class EdgeCases {

        @Test
        @DisplayName("Debe lanzar NullPointerException si command es null")
        void shouldRejectNullCommand() {
            assertThrows(NullPointerException.class, () -> service.execute(null));
            verifyNoInteractions(orderRepository);
            verifyNoInteractions(notificationPort);
        }

        @Test
        @DisplayName("Debe funcionar sin repartidor asignado")
        void shouldHandleNoDriverAssigned() {
            Order order = new Order(
                    ORDER_ID, CUSTOMER_ID, MERCHANT_ID, null,
                    OrderStatus.ON_THE_WAY,
                    "Pickup", null, "Delivery", null,
                    null, null,
                    new BigDecimal("100.00"),
                    DeliveryPin.generate(),
                    null,
                    java.time.Instant.now(), java.time.Instant.now(),
                    new ArrayList<>()
            );

            String pin = order.getDeliveryPin().getValue();
            var command = new ValidateDeliveryPinCommand(ORDER_ID, pin);

            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            assertTrue(service.execute(command));

            verify(notificationPort).notifyCustomer(
                    eq(CUSTOMER_ID), eq(NotificationType.DELIVERY_CONFIRMED), any());
            verify(notificationPort, never()).notifyDriver(any(), any(), any());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  🔧 CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("🔧 Constructor — Validaciones")
    class ConstructorValidation {

        @Test
        @DisplayName("Debe rechazar orderRepository null")
        void shouldRejectNullRepository() {
            assertThrows(NullPointerException.class,
                    () -> new ValidateDeliveryPinService(null, notificationPort));
        }

        @Test
        @DisplayName("Debe rechazar notificationPort null")
        void shouldRejectNullNotificationPort() {
            assertThrows(NullPointerException.class,
                    () -> new ValidateDeliveryPinService(orderRepository, null));
        }
    }
}
