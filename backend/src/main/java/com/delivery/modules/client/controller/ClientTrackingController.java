package com.delivery.modules.client.controller;

import com.delivery.modules.client.dto.OrderTrackingResponseDTO;
import com.delivery.modules.client.service.ClientTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controlador Spring Boot para el Módulo CLIENT (Tracking en Vivo & PIN).
 *
 * <p>Equivalente en el backend de la vista de seguimiento de pedidos en Tulcán.</p>
 */
@RestController
@RequestMapping({"/v1/modules/client/tracking", "/v1/client/tracking"})
@PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')")
@Tag(name = "Módulo Cliente - Seguimiento", description = "Seguimiento en vivo, coordenadas GPS de Redis y PIN OTP")
public class ClientTrackingController {

    private final ClientTrackingService service;

    public ClientTrackingController(ClientTrackingService service) {
        this.service = service;
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Consultar estado de seguimiento y PIN de entrega")
    public ResponseEntity<OrderTrackingResponseDTO> getOrderTracking(@PathVariable UUID orderId) {
        return service.getOrderTracking(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
