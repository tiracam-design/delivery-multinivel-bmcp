package com.delivery.modules.driver.controller;

import com.delivery.modules.driver.dto.ActiveTripResponseDTO;
import com.delivery.modules.driver.dto.PinValidationRequestDTO;
import com.delivery.modules.driver.service.DriverActiveTripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Controlador Spring Boot para el Módulo DRIVER (Active Trip).
 *
 * <p>Expone endpoints para la pantalla móvil del repartidor: viaje activo,
 * botón de incidente / emergencia y validación de PIN OTP de 4 dígitos.</p>
 */
@RestController
@RequestMapping({"/v1/modules/driver/active-trip", "/v1/driver/active-trip"})
@PreAuthorize("hasRole('DRIVER')")
@Tag(name = "Módulo Repartidor - Viaje Activo", description = "Operativa de entrega móvil, incidente y PIN de entrega")
public class DriverActiveTripController {

    private final DriverActiveTripService service;

    public DriverActiveTripController(DriverActiveTripService service) {
        this.service = service;
    }

    @GetMapping("/{driverId}")
    @Operation(summary = "Consultar viaje activo del repartidor")
    public ResponseEntity<ActiveTripResponseDTO> getActiveTrip(@PathVariable UUID driverId) {
        return service.getActiveTripForDriver(driverId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/{orderId}/incident")
    @Operation(summary = "Pausar entrega por incidente / emergencia")
    public ResponseEntity<?> reportIncident(
            @PathVariable UUID orderId,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.getOrDefault("reason", "Incidente en ruta") : "Incidente en ruta";
        boolean paused = service.reportIncident(orderId, reason);
        if (paused) {
            return ResponseEntity.ok(Map.of(
                    "orderId", orderId,
                    "status", "INCIDENT_PAUSED",
                    "message", "Viaje pausado por incidente reportado a central"
            ));
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{orderId}/validate-pin")
    @Operation(summary = "Validar PIN de 4 dígitos y confirmar entrega")
    public ResponseEntity<?> validatePin(
            @PathVariable UUID orderId,
            @Valid @RequestBody PinValidationRequestDTO request) {
        boolean confirmed = service.validatePin(orderId, request.pin());
        return ResponseEntity.ok(Map.of(
                "orderId", orderId,
                "status", "DELIVERED",
                "confirmed", confirmed,
                "message", "Entrega completada exitosamente y fondos liberados"
        ));
    }
}
