package com.delivery.modules.merchant.controller;

import com.delivery.modules.merchant.dto.KitchenOrderResponseDTO;
import com.delivery.modules.merchant.service.MerchantKitchenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controlador Spring Boot para el Módulo MERCHANT (Kitchen Dashboard & KDS).
 *
 * <p>Equivalente en el backend para la recepción de pedidos en cocina,
 * tiempos de preparación, control de existencias, control de saturación operativa
 * (throttle) y resumen de ventas / finanzas diarias ($ USD).</p>
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping({"/v1/modules/merchant/kitchen", "/v1/merchant/kitchen", "/api/v1/merchant/kitchen", "/api/v1/modules/merchant/kitchen"})
@PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')")
@Tag(name = "Módulo Comercio - Cocina", description = "Dashboard de cocina KDS, órdenes entrantes, control de stock y caja diaria")
public class MerchantKitchenController {

    private final MerchantKitchenService service;

    public MerchantKitchenController(MerchantKitchenService service) {
        this.service = service;
    }

    @GetMapping("/orders/pending")
    @Operation(summary = "Pedidos pendientes de cocina")
    public ResponseEntity<List<KitchenOrderResponseDTO>> getPendingOrders() {
        return ResponseEntity.ok(service.getPendingKitchenOrders());
    }

    @PostMapping("/orders/{orderId}/accept")
    @Operation(summary = "Aceptar pedido e indicar minutos de preparación")
    public ResponseEntity<?> acceptOrder(
            @PathVariable UUID orderId,
            @RequestBody(required = false) Map<String, Integer> body) {
        int minutes = body != null && body.containsKey("preparationMinutes") ? body.get("preparationMinutes") : 20;
        service.acceptOrder(orderId, minutes);
        return ResponseEntity.ok(Map.of(
                "orderId", orderId,
                "status", "IN_PREPARATION",
                "preparationMinutes", minutes,
                "message", "Pedido en preparación y búsqueda de repartidor iniciada (< 3 km)"
        ));
    }

    @PatchMapping("/products/{productId}/availability")
    @Operation(summary = "Alternar disponibilidad / stock del producto")
    public ResponseEntity<?> toggleProductStock(@PathVariable UUID productId) {
        boolean available = service.toggleProductAvailability(productId);
        return ResponseEntity.ok(Map.of(
                "productId", productId,
                "available", available,
                "status", available ? "AVAILABLE" : "OUT_OF_STOCK"
        ));
    }

    @PostMapping("/pause")
    @Operation(summary = "Control de saturación operativa: pausar o reanudar nuevos pedidos")
    public ResponseEntity<Map<String, Object>> pauseKitchen(
            @RequestBody(required = false) Map<String, Object> body) {
        boolean pause = body == null || !body.containsKey("paused") || Boolean.parseBoolean(String.valueOf(body.get("paused")));
        int minutes = body != null && body.containsKey("durationMinutes")
                ? Integer.parseInt(String.valueOf(body.get("durationMinutes")))
                : 30;

        return ResponseEntity.ok(service.pauseKitchen(pause, minutes));
    }

    @GetMapping("/status")
    @Operation(summary = "Consultar estado operativo y de capacidad de la cocina")
    public ResponseEntity<Map<String, Object>> getKitchenStatus() {
        return ResponseEntity.ok(service.getKitchenStatus());
    }

    @GetMapping("/financial-summary")
    @Operation(summary = "Consultar resumen financiero y cierre de caja diario en USD")
    public ResponseEntity<Map<String, Object>> getFinancialSummary() {
        return ResponseEntity.ok(service.getFinancialSummary());
    }
}
