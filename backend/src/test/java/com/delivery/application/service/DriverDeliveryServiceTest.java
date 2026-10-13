package com.delivery.application.service;

import com.delivery.domain.enums.NotificationType;
import com.delivery.domain.enums.OrderStatus;
import com.delivery.domain.exception.InvalidDeliveryPinException;
import com.delivery.domain.exception.InvalidStateTransitionException;
import com.delivery.domain.exception.OrderNotFoundException;
import com.delivery.domain.model.GeoLocation;
import com.delivery.domain.model.Order;
import com.delivery.domain.port.output.DriverWalletPort;
import com.delivery.domain.port.output.NotificationPort;
import com.delivery.domain.port.output.OrderRepositoryPort;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DriverDeliveryService — Tests Unitarios")
class DriverDeliveryServiceTest {

    @Mock
    private OrderRepositoryPort orderRepository;

    @Mock
    private DriverWalletPort driverWalletPort;

    @Mock
    private NotificationPort notificationPort;

    @Mock
    private SimpMessageSendingOperations messagingTemplate;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    private DriverDeliveryService service;

    private final UUID orderId = UUID.randomUUID();
    private final UUID driverId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();
    private final UUID merchantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new DriverDeliveryService(
                orderRepository,
                driverWalletPort,
                notificationPort,
                messagingTemplate
        );
    }

    private Order createTestOrder() {
        return Order.create(
                customerId,
                merchantId,
                "Calle Sucre 14-20 y Pichincha, Parque Central, Tulcán",
                new GeoLocation(0.8116, -77.7172),
                "Av. Coral 450 y Av. Manabí, Terminal Terrestre, Tulcán",
                new GeoLocation(0.8035, -77.7245),
                new BigDecimal("12.50"),
                "Sin cebolla"
        );
    }

    @Test
    @DisplayName("acceptTrip: debe asignar el conductor, cambiar a ACCEPTED y notificar")
    void shouldAcceptTripSuccessfully() {
        Order order = createTestOrder();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        service.acceptTrip(orderId, driverId);

        verify(orderRepository).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertEquals(driverId, savedOrder.getDriverId());
        assertEquals(OrderStatus.ACCEPTED, savedOrder.getStatus());

        // Verificaciones de notificación
        verify(notificationPort).notifyCustomer(eq(customerId), eq(NotificationType.DRIVER_ASSIGNED), any());
        verify(notificationPort).notifyMerchant(eq(merchantId), eq(NotificationType.STATUS_UPDATED), any());

        // Verificación de WebSocket STOMP
        verify(messagingTemplate).convertAndSend(eq("/topic/orders/" + orderId + "/tracking"), any(Map.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/orders/" + orderId + "/status"), any(Map.class));
    }

    @Test
    @DisplayName("acceptTrip: debe lanzar OrderNotFoundException si el pedido no existe")
    void shouldThrowNotFoundOnAcceptTrip() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> service.acceptTrip(orderId, driverId));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("validateDeliveryPin: PIN correcto pasa a DELIVERED y libera saldo")
    void shouldValidatePinSuccessfullyAndReleaseEscrow() {
        Order order = createTestOrder();
        order.assignDriver(driverId);
        // Avanzar el flujo para que esté en ON_THE_WAY
        order.transitionTo(OrderStatus.ACCEPTED);
        order.transitionTo(OrderStatus.IN_PREPARATION);
        order.transitionTo(OrderStatus.READY_FOR_PICKUP);
        order.transitionTo(OrderStatus.ON_THE_WAY);

        String correctPin = order.getDeliveryPin().getValue();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        boolean result = service.validateDeliveryPin(orderId, correctPin);

        assertTrue(result);
        verify(orderRepository).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertEquals(OrderStatus.DELIVERED, savedOrder.getStatus());

        // Verifica liberación de saldo a la billetera del repartidor
        verify(driverWalletPort).releaseEscrowFunds(eq(orderId), eq(driverId), eq(new BigDecimal("250.00")));

        // Verifica notificaciones
        verify(notificationPort).notifyCustomer(eq(customerId), eq(NotificationType.DELIVERY_CONFIRMED), any());
        verify(notificationPort).notifyDriver(eq(driverId), eq(NotificationType.DELIVERY_CONFIRMED), any());

        // Verifica difusión WebSocket
        verify(messagingTemplate).convertAndSend(eq("/topic/orders/" + orderId + "/tracking"), any(Map.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/orders/" + orderId + "/status"), any(Map.class));
    }

    @Test
    @DisplayName("validateDeliveryPin: PIN incorrecto lanza InvalidDeliveryPinException y no libera fondos")
    void shouldFailOnIncorrectPin() {
        Order order = createTestOrder();
        order.assignDriver(driverId);
        order.transitionTo(OrderStatus.ACCEPTED);
        order.transitionTo(OrderStatus.IN_PREPARATION);
        order.transitionTo(OrderStatus.READY_FOR_PICKUP);
        order.transitionTo(OrderStatus.ON_THE_WAY);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(InvalidDeliveryPinException.class, () -> service.validateDeliveryPin(orderId, "0000"));

        // Guarda el intento fallido (para rate-limit/bloqueo a los 3 intentos)
        verify(orderRepository).save(order);
        // NO debe liberar el saldo
        verify(driverWalletPort, never()).releaseEscrowFunds(any(), any(), any());
    }

    @Test
    @DisplayName("validateDeliveryPin: PIN que no tiene 4 dígitos lanza IllegalArgumentException")
    void shouldThrowIllegalArgumentWhenPinFormatInvalid() {
        assertThrows(IllegalArgumentException.class, () -> service.validateDeliveryPin(orderId, "123"));
        assertThrows(IllegalArgumentException.class, () -> service.validateDeliveryPin(orderId, "12345"));
        assertThrows(IllegalArgumentException.class, () -> service.validateDeliveryPin(orderId, "abcd"));
        verify(orderRepository, never()).findById(any());
    }

    @Test
    @DisplayName("validateDeliveryPin: Orden en estado inválido (CREATED) lanza InvalidStateTransitionException")
    void shouldThrowInvalidStateTransitionWhenOrderNotOnTheWay() {
        Order order = createTestOrder();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(InvalidStateTransitionException.class, () -> service.validateDeliveryPin(orderId, "1234"));
        verify(driverWalletPort, never()).releaseEscrowFunds(any(), any(), any());
    }
}
