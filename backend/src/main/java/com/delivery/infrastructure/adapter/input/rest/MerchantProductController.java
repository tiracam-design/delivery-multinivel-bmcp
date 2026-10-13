package com.delivery.infrastructure.adapter.input.rest;

import com.delivery.domain.port.input.ToggleProductStockUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Adaptador de Entrada REST — Gestión de Productos y Stock del Comercio.
 *
 * <p>Expone los endpoints para que los comercios actualicen la disponibilidad
 * y stock de sus productos en tiempo real.</p>
 *
 * <h3>Ruta base:</h3>
 * {@code /api/v1/merchant/products}
 *
 * @see ToggleProductStockUseCase
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/merchant/products")
@PreAuthorize("hasRole('MERCHANT')")
@Tag(name = "Módulo Comercio - Productos", description = "Endpoints para la gestión del catálogo de productos y stock")
@SecurityRequirement(name = "bearerAuth")
public class MerchantProductController {

    private final ToggleProductStockUseCase toggleProductStockUseCase;

    public MerchantProductController(ToggleProductStockUseCase toggleProductStockUseCase) {
        this.toggleProductStockUseCase = Objects.requireNonNull(toggleProductStockUseCase,
                "toggleProductStockUseCase no puede ser null");
    }

    /**
     * Alterna la disponibilidad (stock activo/pausado) de un producto.
     *
     * @param productId ID del producto a alternar
     * @return nuevo estado de disponibilidad
     */
    @PatchMapping("/{productId}/availability")
    @Operation(
            summary = "Alternar disponibilidad/stock de producto",
            description = "Cambia el estado disponible de un producto de forma atómica (disponible ↔ agotado)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Disponibilidad del producto actualizada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado — requiere rol MERCHANT")
    })
    public ResponseEntity<Map<String, Object>> toggleAvailability(
            @Parameter(description = "ID del producto", required = true)
            @PathVariable("productId") UUID productId) {

        boolean isAvailable = toggleProductStockUseCase.toggleAvailability(productId);

        return ResponseEntity.ok(Map.of(
                "productId", productId,
                "available", isAvailable,
                "status", isAvailable ? "AVAILABLE" : "OUT_OF_STOCK",
                "message", isAvailable
                        ? "Producto marcado como disponible para pedidos"
                        : "Producto pausado por falta de stock",
                "updatedAt", Instant.now().toString()
        ));
    }

    /**
     * Endpoint alternativo POST para compatibilidad con clientes que no soportan PATCH.
     */
    @PostMapping("/{productId}/toggle-availability")
    @Operation(summary = "Alternar disponibilidad (alias POST)")
    public ResponseEntity<Map<String, Object>> toggleAvailabilityPost(
            @PathVariable("productId") UUID productId) {
        return toggleAvailability(productId);
    }
}
