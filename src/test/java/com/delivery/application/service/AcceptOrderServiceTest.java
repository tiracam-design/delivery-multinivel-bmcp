package com.delivery.application.service;

import com.delivery.domain.enums.NotificationType;
import com.delivery.domain.enums.OrderStatus;
import com.delivery.domain.exception.OrderNotFoundException;
import com.delivery.domain.model.Driver;
import com.delivery.domain.model.GeoLocation;
import com.delivery.domain.model.Order;
import com.delivery.domain.port.input.AssignNearestDriverUseCase;
import com.delivery.domain.port.output.DriverGeolocationPort;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AcceptOrderService — Tests Unitarios")
class AcceptOrderServiceTest {

    @Mock
    private OrderRepositoryPort orderRepository;

    @Mock
    private DriverGeolocationPort driverGeolocationPort;

    @Mock
    private AssignNearestDriverUseCase assignNearestDriverUseCase;

    @Mock
    private SimpMessageSendingOperations messagingTemplate;

    @Mock
    private NotificationPort notificationPort;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    @Captor
    private ArgumentCaptor<Map<String, Object>> payloadCaptor;

    private AcceptOrderService service;

    private final UUID orderId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();
    private final UUID merchantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AcceptOrderService(
                orderRepository,
                driverGeolocationPort,
                assignNearestDriverUseCase,
                messagingTemplate,
                notificationPort
        );
    }

    @Test
    @DisplayName("acceptOrder: debe transicionar a IN_PREPARATION, buscar choferes < 3km y notificar por WebSocket")
    void shouldAcceptOrderSuccessfully() {
        // Arrange
        GeoLocation pickup = new GeoLocation(0.8116, -77.7172);
        GeoLocation delivery = new GeoLocation(0.8035, -77.7245);
        Order order = new Order(
                orderId,
                customerId,
                merchantId,
                null,
                OrderStatus.CREATED,
                "Calle Sucre 14-20 y Pichincha, Parque Central, Tulcán",
                pickup,
                "Av. Coral 450 y Av. Manabí, Terminal Terrestre, Tulcán",
                delivery,
                2.5,
                null,
                new BigDecimal("12.50"),
                com.delivery.domain.model.DeliveryPin.generate(),
                "Sin picante",
                java.time.Instant.now(),
                java.time.Instant.now(),
                null
        );

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(driverGeolocationPort.findAvailableDriversWithinRadius(eq(pickup), eq(3.0)))
                .thenReturn(List.of());

        // Act
        service.acceptOrder(orderId, 25);

        // Assert
        verify(orderRepository).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertEquals(OrderStatus.IN_PREPARATION, savedOrder.getStatus());
        assertEquals(25, savedOrder.getEstimatedDurationMinutes());

        // Verificación de búsqueda geoespacial PostGIS
        verify(driverGeolocationPort).findAvailableDriversWithinRadius(pickup, 3.0);

        // Verificación de WebSocket
        String expectedTopic = "/topic/orders/" + savedOrder.getId() + "/tracking";
        verify(messagingTemplate).convertAndSend(eq(expectedTopic), any(Map.class));

        // Verificación de notificación al cliente
        verify(notificationPort).notifyCustomer(eq(customerId), eq(NotificationType.STATUS_UPDATED), payloadCaptor.capture());
        Map<String, Object> payload = payloadCaptor.getValue();
        assertEquals("IN_PREPARATION", payload.get("status"));
        assertEquals(25, payload.get("preparationMinutes"));
        assertNotNull(payload.get("estimatedDeliveryTime"));
    }

    @Test
    @DisplayName("acceptOrder: debe lanzar OrderNotFoundException si la orden no existe")
    void shouldThrowExceptionWhenOrderNotFound() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> service.acceptOrder(orderId, 30));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("acceptOrder: debe rechazar minutos de preparación menores o iguales a cero")
    void shouldRejectInvalidMinutes() {
        assertThrows(IllegalArgumentException.class, () -> service.acceptOrder(orderId, 0));
        assertThrows(IllegalArgumentException.class, () -> service.acceptOrder(orderId, -5));
    }
}
