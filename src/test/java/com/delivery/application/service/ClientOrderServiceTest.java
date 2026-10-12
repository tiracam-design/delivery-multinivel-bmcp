package com.delivery.application.service;

import com.delivery.application.dto.OrderResponseDTO;
import com.delivery.domain.enums.OrderStatus;
import com.delivery.domain.model.GeoLocation;
import com.delivery.domain.model.Order;
import com.delivery.domain.port.input.command.CreateOrderCommand;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClientOrderService — Tests Unitarios")
class ClientOrderServiceTest {

    @Mock
    private OrderRepositoryPort orderRepository;

    @Mock
    private SimpMessageSendingOperations messagingTemplate;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    @Captor
    private ArgumentCaptor<OrderResponseDTO> wsPayloadCaptor;

    private ClientOrderService service;

    private final UUID customerId = UUID.randomUUID();
    private final UUID merchantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ClientOrderService(orderRepository, messagingTemplate);
    }

    @Test
    @DisplayName("createOrder: debe calcular envío con PostGIS, generar PIN 4 dígitos, estado CREATED y notificar WebSocket")
    void shouldCreateOrderSuccessfully() {
        // Arrange
        GeoLocation pickup = new GeoLocation(0.8116, -77.7172);
        GeoLocation delivery = new GeoLocation(0.8035, -77.7245);
        CreateOrderCommand command = new CreateOrderCommand(
                customerId,
                merchantId,
                "Calle Sucre 14-20 y Pichincha, Parque Central, Tulcán",
                pickup,
                "Av. Coral 450 y Av. Manabí, Terminal Terrestre, Tulcán",
                delivery,
                new BigDecimal("12.50"),
                "Entregar en recepción"
        );

        when(orderRepository.calculateDistanceKm(pickup, delivery)).thenReturn(4.5);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        OrderResponseDTO response = service.createOrder(command);

        // Assert
        assertNotNull(response);
        assertEquals(customerId, response.customerId());
        assertEquals(merchantId, response.merchantId());
        assertEquals("CREATED", response.status());

        // Verificación de PIN de 4 dígitos
        assertNotNull(response.deliveryPin());
        assertEquals(4, response.deliveryPin().length());
        assertTrue(response.deliveryPin().matches("\\d{4}"), "El PIN debe tener exactamente 4 dígitos");

        // Verificación de tarifa de envío: 2.50 + (4.5 * 1.20) = 2.50 + 5.40 = 7.90
        assertEquals(new BigDecimal("7.90"), response.shippingCost());
        // Total = 100.00 + 7.90 = 107.90
        assertEquals(new BigDecimal("107.90"), response.totalAmount());

        // Verificación de persistencia
        verify(orderRepository).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertEquals(OrderStatus.CREATED, savedOrder.getStatus());
        assertEquals(new BigDecimal("107.90"), savedOrder.getTotalAmount());

        // Verificación de notificación WebSocket al canal del comercio
        String expectedTopic = "/topic/merchant/" + merchantId + "/orders";
        verify(messagingTemplate).convertAndSend(eq(expectedTopic), wsPayloadCaptor.capture());
        OrderResponseDTO wsPayload = wsPayloadCaptor.getValue();
        assertEquals(expectedTopic, "/topic/merchant/" + wsPayload.merchantId() + "/orders");
        assertEquals("CREATED", wsPayload.status());
    }

    @Test
    @DisplayName("getClientActiveOrders: debe retornar solo pedidos en estados activos")
    void shouldReturnOnlyActiveOrders() {
        // Arrange
        Order activeOrder1 = Order.create(
                customerId, merchantId, "Dir 1", null, "Dir 2", null,
                new BigDecimal("50.00"), null
        );
        Order activeOrder2 = Order.create(
                customerId, merchantId, "Dir 3", null, "Dir 4", null,
                new BigDecimal("75.00"), null
        );
        activeOrder2.transitionTo(OrderStatus.ACCEPTED);

        Order cancelledOrder = Order.create(
                customerId, merchantId, "Dir 5", null, "Dir 6", null,
                new BigDecimal("30.00"), null
        );
        cancelledOrder.transitionTo(OrderStatus.CANCELLED);

        when(orderRepository.findByCustomerId(customerId))
                .thenReturn(List.of(activeOrder1, activeOrder2, cancelledOrder));

        // Act
        List<OrderResponseDTO> activeOrders = service.getClientActiveOrders(customerId);

        // Assert
        assertEquals(2, activeOrders.size());
        assertTrue(activeOrders.stream().noneMatch(o -> "CANCELLED".equals(o.status())));
    }
}
