package com.delivery.infrastructure.adapter.input.rest;

import com.delivery.domain.port.input.DriverDeliveryUseCase;
import com.delivery.infrastructure.adapter.input.rest.dto.AcceptTripRequest;
import com.delivery.infrastructure.adapter.input.rest.dto.DriverValidatePinRequest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DriverOrderController — Tests Unitarios")
class DriverOrderControllerTest {

    @Mock
    private DriverDeliveryUseCase driverDeliveryUseCase;

    @InjectMocks
    private DriverOrderController controller;

    @Test
    @DisplayName("DriverOrderController debe estar protegido con @PreAuthorize(\"hasRole('DRIVER')\")")
    void shouldHavePreAuthorizeDriverRole() {
        PreAuthorize annotation = DriverOrderController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation);
        assertEquals("hasRole('DRIVER')", annotation.value());
    }

    @Test
    @DisplayName("acceptTrip: debe invocar al caso de uso y retornar 200 OK con estado ACCEPTED")
    void shouldAcceptTripSuccessfully() {
        UUID orderId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        AcceptTripRequest request = new AcceptTripRequest(driverId);

        ResponseEntity<Map<String, Object>> response = controller.acceptTrip(orderId, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ACCEPTED", response.getBody().get("status"));
        assertEquals(orderId, response.getBody().get("orderId"));
        assertEquals(driverId, response.getBody().get("driverId"));

        verify(driverDeliveryUseCase).acceptTrip(orderId, driverId);
    }

    @Test
    @DisplayName("validateDeliveryPin: debe invocar al caso de uso y retornar 200 OK con confirmación y saldo liberado")
    void shouldValidatePinSuccessfully() {
        UUID orderId = UUID.randomUUID();
        DriverValidatePinRequest request = new DriverValidatePinRequest("4829");

        when(driverDeliveryUseCase.validateDeliveryPin(orderId, "4829")).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = controller.validateDeliveryPin(orderId, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("DELIVERED", response.getBody().get("status"));
        assertEquals(true, response.getBody().get("confirmed"));
        assertEquals(true, response.getBody().get("escrowReleased"));

        verify(driverDeliveryUseCase).validateDeliveryPin(orderId, "4829");
    }
}
