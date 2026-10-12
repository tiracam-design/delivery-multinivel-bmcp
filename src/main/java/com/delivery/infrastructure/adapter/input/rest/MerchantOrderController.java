package com.delivery.infrastructure.adapter.input.rest;

import com.delivery.domain.port.input.AcceptOrderUseCase;
import com.delivery.infrastructure.adapter.input.rest.dto.AcceptOrderRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Adaptador de Entrada REST — Gestión de Pedidos del Comercio.
 *
 * <p>Expone los endpoints para que los comercios (restaurantes) acepten pedidos,
 * inicien su preparación y coordinen la búsqueda de repartidores.</p>
 *
 * <h3>Ruta base:</h3>
 * {@code /api/v1/merchant/orders}
 *
 * @see AcceptOrderUseCase
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/merchant/orders")
@PreAuthorize("hasRole('MERCHANT')")
@Tag(name = "Módulo Comercio - Pedidos", description = "Endpoints para la gestión de pedidos por el comercio")
@SecurityRequirement(name = "bearerAuth")
public class MerchantOrderController {

    private final AcceptOrderUseCase acceptOrderUseCase;

    public MerchantOrderController(AcceptOrderUseCase acceptOrderUseCase) {
        this.acceptOrderUseCase = Objects.requireNonNull(acceptOrderUseCase,
                "acceptOrderUseCase no puede ser null");
    }

    /**
     * Acepta un pedido y lo coloca en preparación.
     *
     * <p>Cambia el estado a {@code IN_PREPARATION}, dispara la búsqueda
     * espacial PostGIS de repartidores a &lt; 3 km y notifica al cliente.</p>
     *
     * @param orderId ID del pedido
     * @param request datos con los minutos estimados de preparación
     * @return confirmación de la aceptación
     */
    @PostMapping("/{orderId}/accept")
    @Operation(
            summary = "Aceptar pedido e iniciar preparación",
            description = "Transiciona el pedido a IN_PREPARATION, busca repartidores a < 3 km vía PostGIS y notifica al cliente por WebSocket."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido aceptado en preparación exitosamente"),
            @ApiResponse(responseCode = "400", description = "Minutos de preparación inválidos o estado inconsistente"),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado — requiere rol MERCHANT")
    })
    public ResponseEntity<Map<String, Object>> acceptOrder(
            @Parameter(description = "ID del pedido a aceptar", required = true)
            @PathVariable("orderId") UUID orderId,
            @Valid @RequestBody AcceptOrderRequest request) {

        acceptOrderUseCase.acceptOrder(orderId, request.preparationMinutes());

        return ResponseEntity.ok(Map.of(
                "orderId", orderId,
                "status", "IN_PREPARATION",
                "preparationMinutes", request.preparationMinutes(),
                "message", "Pedido aceptado y en preparación. Búsqueda de repartidores iniciada (< 3 km).",
                "acceptedAt", Instant.now().toString()
        ));
    }
}
