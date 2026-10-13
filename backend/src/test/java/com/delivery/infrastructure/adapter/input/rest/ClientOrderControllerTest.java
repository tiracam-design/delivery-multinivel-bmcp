package com.delivery.infrastructure.adapter.input.rest;

import com.delivery.application.dto.OrderResponseDTO;
import com.delivery.domain.port.input.ClientOrderUseCase;
import com.delivery.domain.port.input.command.CreateOrderCommand;
import com.delivery.infrastructure.adapter.input.rest.dto.CreateOrderRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClientOrderController — Tests Unitarios")
class ClientOrderControllerTest {

    @Mock
    private ClientOrderUseCase clientOrderUseCase;

    @Captor
    private ArgumentCaptor<CreateOrderCommand> commandCaptor;

    private ClientOrderController controller;

    private final UUID customerId = UUID.randomUUID();
    private final UUID merchantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        controller = new ClientOrderController(clientOrderUseCase);
    }

    @Test
    @DisplayName("La clase debe estar protegida con @PreAuthorize(\"hasRole('CLIENT')\")")
    void shouldHavePreAuthorizeAnnotation() {
        PreAuthorize annotation = ClientOrderController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation, "Debe tener anotación @PreAuthorize a nivel de clase");
        assertEquals("hasRole('CLIENT')", annotation.value());
    }

    @Test
    @DisplayName("createOrder: debe mapear DTO a comando y retornar 201 CREATED con OrderResponseDTO")
    void shouldCreateOrderAndReturnCreated() {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(
                customerId,
                merchantId,
                "Calle Sucre 14-20 y Pichincha, Parque Central, Tulcán",
                0.8116,
                -77.7172,
                "Av. Coral 450 y Av. Manabí, Terminal Terrestre, Tulcán",
                0.8035,
                -77.7245,
                new BigDecimal("12.50"),
                "Sin cebolla"
        );

        OrderResponseDTO mockResponse = new OrderResponseDTO(
                UUID.randomUUID(),
                customerId,
                merchantId,
                null,
                "CREATED",
                request.pickupAddress(),
                request.pickupLatitude(),
                request.pickupLongitude(),
                request.deliveryAddress(),
                request.deliveryLatitude(),
                request.deliveryLongitude(),
                2.5,
                15,
                new BigDecimal("1.50"),
                new BigDecimal("14.00"),
                "1234",
                request.notes(),
                Instant.now(),
                Instant.now()
        );

        when(clientOrderUseCase.createOrder(any(CreateOrderCommand.class))).thenReturn(mockResponse);

        // Act
        ResponseEntity<OrderResponseDTO> response = controller.createOrder(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(mockResponse.id(), response.getBody().id());
        assertEquals("CREATED", response.getBody().status());
        assertEquals("1234", response.getBody().deliveryPin());
        assertEquals(new BigDecimal("14.00"), response.getBody().totalAmount());

        verify(clientOrderUseCase).createOrder(commandCaptor.capture());
        CreateOrderCommand capturedCommand = commandCaptor.getValue();
        assertEquals(customerId, capturedCommand.getCustomerId());
        assertEquals(merchantId, capturedCommand.getMerchantId());
        assertEquals("Calle Sucre 14-20 y Pichincha, Parque Central, Tulcán", capturedCommand.getPickupAddress());
        assertNotNull(capturedCommand.getPickupLocation());
        assertEquals(0.8116, capturedCommand.getPickupLocation().getLatitude());
        assertEquals(-77.7172, capturedCommand.getPickupLocation().getLongitude());
    }

    @Test
    @DisplayName("getActiveOrders: debe delegar en getClientActiveOrders y retornar 200 OK")
    void shouldGetActiveOrdersSuccessfully() {
        // Arrange
        OrderResponseDTO mockOrder = new OrderResponseDTO(
                UUID.randomUUID(),
                customerId,
                merchantId,
                null,
                "CREATED",
                "Calle A",
                null,
                null,
                "Calle B",
                null,
                null,
                null,
                null,
                new BigDecimal("5.00"),
                new BigDecimal("55.00"),
                "9876",
                null,
                Instant.now(),
                Instant.now()
        );

        when(clientOrderUseCase.getClientActiveOrders(customerId)).thenReturn(List.of(mockOrder));

        // Act
        ResponseEntity<List<OrderResponseDTO>> response = controller.getActiveOrders(customerId, null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(mockOrder.id(), response.getBody().get(0).id());

        verify(clientOrderUseCase).getClientActiveOrders(customerId);
    }

    @Test
    @DisplayName("getActiveOrdersByPath: debe resolver por path variable y retornar 200 OK")
    void shouldGetActiveOrdersByPathSuccessfully() {
        // Arrange
        when(clientOrderUseCase.getClientActiveOrders(customerId)).thenReturn(List.of());

        // Act
        ResponseEntity<List<OrderResponseDTO>> response = controller.getActiveOrdersByPath(customerId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());

        verify(clientOrderUseCase).getClientActiveOrders(customerId);
    }
}
