package com.delivery.infrastructure.adapter.output.redis.listener;

import com.delivery.domain.enums.NotificationType;
import com.delivery.domain.enums.OrderStatus;
import com.delivery.domain.model.GeoLocation;
import com.delivery.domain.model.Order;
import com.delivery.domain.port.output.NotificationPort;
import com.delivery.domain.port.output.OrderRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.Message;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderUnacceptedExpirationListener — Expiración de pedidos por Keyspace Notifications")
class OrderUnacceptedExpirationListenerTest {

    @Mock
    private OrderRepositoryPort orderRepositoryPort;

    @Mock
    private NotificationPort notificationPort;

    private OrderUnacceptedExpirationListener listener;

    @BeforeEach
    void setUp() {
        listener = new OrderUnacceptedExpirationListener(orderRepositoryPort, notificationPort);
    }

    @Test
    @DisplayName("Cuando expira order:unaccepted:{orderId} y pedido sigue en CREATED, cancela y notifica al cliente")
    void onMessage_whenUnacceptedOrderExpires_cancelsOrderAndNotifiesCustomer() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();

        Order order = new Order(
                orderId,
                customerId,
                merchantId,
                null,
                OrderStatus.CREATED,
                "Av. Principal 123",
                new GeoLocation(-0.81, -77.71),
                "Calle Los Pinos 456",
                new GeoLocation(-0.82, -77.72),
                2.5,
                15,
                new BigDecimal("45.00"),
                null,
                "Sin cebolla",
                java.time.Instant.now(),
                java.time.Instant.now(),
                new java.util.ArrayList<>()
        );

        when(orderRepositoryPort.findById(orderId)).thenReturn(Optional.of(order));

        String expiredKey = "order:unaccepted:" + orderId;
        Message message = new DefaultMessage(
                "__keyevent@0__:expired".getBytes(StandardCharsets.UTF_8),
                expiredKey.getBytes(StandardCharsets.UTF_8)
        );

        listener.onMessage(message, null);

        // Verifica que el pedido pasó a CANCELLED
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        verify(orderRepositoryPort).save(order);

        // Verifica notificación al cliente
        verify(notificationPort).notifyCustomer(
                eq(customerId),
                eq(NotificationType.ORDER_CANCELLED),
                any()
        );

        // Verifica notificación al restaurante
        verify(notificationPort).notifyMerchant(
                eq(merchantId),
                eq(NotificationType.ORDER_CANCELLED),
                any()
        );
    }

    @Test
    @DisplayName("Ignora claves que no corresponden a expiración de pedidos no aceptados")
    void onMessage_ignoresUnrelatedKeys() {
        String otherKey = "driver:location:abc-123";
        Message message = new DefaultMessage(
                "__keyevent@0__:expired".getBytes(StandardCharsets.UTF_8),
                otherKey.getBytes(StandardCharsets.UTF_8)
        );

        listener.onMessage(message, null);

        verifyNoInteractions(orderRepositoryPort);
        verifyNoInteractions(notificationPort);
    }
}
