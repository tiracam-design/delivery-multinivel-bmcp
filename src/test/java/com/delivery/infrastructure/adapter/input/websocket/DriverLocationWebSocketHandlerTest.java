package com.delivery.infrastructure.adapter.input.websocket;

import com.delivery.domain.model.GeoLocation;
import com.delivery.domain.port.output.DriverGeolocationPort;
import com.delivery.infrastructure.adapter.input.websocket.dto.DriverLocationMessage;
import com.delivery.infrastructure.adapter.input.websocket.dto.DriverLocationUpdate;
import com.delivery.infrastructure.adapter.input.websocket.DriverLocationSecurityValidator.ValidationResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DriverLocationWebSocketHandler — Tests Unitarios")
class DriverLocationWebSocketHandlerTest {

    @Mock
    private SimpMessageSendingOperations messagingTemplate;

    @Mock
    private DriverLocationSecurityValidator securityValidator;

    @Mock
    private DriverGeolocationPort driverGeolocationPort;

    @Captor
    private ArgumentCaptor<DriverLocationUpdate> updateCaptor;

    private DriverLocationWebSocketHandler handler;

    private final UUID driverId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new DriverLocationWebSocketHandler(
                messagingTemplate,
                securityValidator,
                driverGeolocationPort
        );
    }

    @Test
    @DisplayName("handleDriverLocation: retransmite coordenadas cada 3s a /topic/orders/{orderId}/tracking y persiste en PostGIS")
    void shouldHandleAndBroadcastDriverLocation() {
        DriverLocationMessage message = new DriverLocationMessage(driverId, orderId, 0.8116, -77.7172);
        when(securityValidator.validate(driverId, orderId)).thenReturn(ValidationResult.authorized());

        handler.handleDriverLocation(message);

        // Verifica persistencia espacial en PostGIS
        verify(driverGeolocationPort).updateDriverLocation(
                eq(driverId),
                eq(new GeoLocation(0.8116, -77.7172)),
                isNull(),
                isNull()
        );

        // Verifica retransmisión al canal principal especificado por el frontend
        verify(messagingTemplate).convertAndSend(
                eq("/topic/orders/" + orderId + "/tracking"),
                updateCaptor.capture()
        );

        DriverLocationUpdate update = updateCaptor.getValue();
        assertEquals(driverId, update.getDriverId());
        assertEquals(orderId, update.getOrderId());
        assertEquals(0.8116, update.getLatitude());
        assertEquals(-77.7172, update.getLongitude());
        assertNotNull(update.getTimestamp());

        // Verifica también el canal de retrocompatibilidad
        verify(messagingTemplate).convertAndSend(
                eq("/topic/tracking/" + orderId),
                any(DriverLocationUpdate.class)
        );
    }

    @Test
    @DisplayName("handleDriverLocation: si la validación es rechazada, envía error a /queue/errors y no retransmite")
    void shouldRejectUnauthorizedLocation() {
        DriverLocationMessage message = new DriverLocationMessage(driverId, orderId, 0.8116, -77.7172);
        when(securityValidator.validate(driverId, orderId))
                .thenReturn(ValidationResult.rejected("Repartidor no asignado"));

        handler.handleDriverLocation(message);

        verify(driverGeolocationPort, never()).updateDriverLocation(any(), any(), any(), any());
        verify(messagingTemplate, never()).convertAndSend(startsWith("/topic/orders/"), any(Object.class));
        verify(messagingTemplate).convertAndSend(eq("/queue/errors"), any(Object.class));
    }
}
