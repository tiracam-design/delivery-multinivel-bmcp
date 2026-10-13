package com.delivery.infrastructure.adapter.input.rest;

import com.delivery.domain.port.input.AcceptOrderUseCase;
import com.delivery.domain.port.input.ToggleProductStockUseCase;
import com.delivery.infrastructure.adapter.input.rest.dto.AcceptOrderRequest;

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
@DisplayName("Merchant Controllers — Tests Unitarios")
class MerchantControllersTest {

    @Mock
    private AcceptOrderUseCase acceptOrderUseCase;

    @Mock
    private ToggleProductStockUseCase toggleProductStockUseCase;

    @Test
    @DisplayName("MerchantOrderController debe estar protegido con @PreAuthorize(\"hasRole('MERCHANT')\")")
    void orderControllerShouldHavePreAuthorizeAnnotation() {
        PreAuthorize annotation = MerchantOrderController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation);
        assertEquals("hasRole('MERCHANT')", annotation.value());
    }

    @Test
    @DisplayName("MerchantProductController debe estar protegido con @PreAuthorize(\"hasRole('MERCHANT')\")")
    void productControllerShouldHavePreAuthorizeAnnotation() {
        PreAuthorize annotation = MerchantProductController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation);
        assertEquals("hasRole('MERCHANT')", annotation.value());
    }

    @Test
    @DisplayName("acceptOrder: debe invocar caso de uso y retornar 200 OK con confirmación")
    void shouldAcceptOrderSuccessfully() {
        MerchantOrderController controller = new MerchantOrderController(acceptOrderUseCase);
        UUID orderId = UUID.randomUUID();
        AcceptOrderRequest request = new AcceptOrderRequest(20);

        ResponseEntity<Map<String, Object>> response = controller.acceptOrder(orderId, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("IN_PREPARATION", response.getBody().get("status"));
        assertEquals(20, response.getBody().get("preparationMinutes"));

        verify(acceptOrderUseCase).acceptOrder(orderId, 20);
    }

    @Test
    @DisplayName("toggleAvailability: debe invocar caso de uso y retornar 200 OK")
    void shouldToggleProductAvailabilitySuccessfully() {
        MerchantProductController controller = new MerchantProductController(toggleProductStockUseCase);
        UUID productId = UUID.randomUUID();

        when(toggleProductStockUseCase.toggleAvailability(productId)).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = controller.toggleAvailability(productId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("available"));
        assertEquals("AVAILABLE", response.getBody().get("status"));

        verify(toggleProductStockUseCase).toggleAvailability(productId);
    }
}
