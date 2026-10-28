package com.delivery.infrastructure.adapter.input.rest;

import com.delivery.domain.port.input.DriverDeliveryUseCase;
import com.delivery.infrastructure.adapter.input.rest.dto.AcceptTripRequest;
import com.delivery.infrastructure.adapter.input.rest.dto.DriverValidatePinRequest;

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
 * Adaptador de Entrada REST — Gestión de Pedidos por el Repartidor.
 *
 * <p>Expone endpoints para la operativa del repartidor:
 * <ul>
 *   <li>Aceptar un viaje / orden asignada.</li>
 *   <li>Validar el PIN de entrega OTP (4 dígitos), transicionar a {@code DELIVERED}
 *       y liberar el saldo en custodia.</li>
 * </ul>
 * </p>
 *
 * <h3>Ruta base:</h3>
 * {@code /api/v1/driver/orders}
 *
 * @see DriverDeliveryUseCase
 * @since 1.0.0
 */
@RestController
@RequestMapping({"/v1/driver/orders", "/api/v1/driver/orders"})
@PreAuthorize("hasRole('DRIVER')")
@Tag(name = "Módulo Repartidor - Pedidos", description = "Endpoints para la operativa del repartidor y validación de entregas")
@SecurityRequirement(name = "bearerAuth")
public class DriverOrderController {

    private final DriverDeliveryUseCase driverDeliveryUseCase;

    public DriverOrderController(DriverDeliveryUseCase driverDeliveryUseCase) {
        this.driverDeliveryUseCase = Objects.requireNonNull(driverDeliveryUseCase,
                "driverDeliveryUseCase no puede ser null");
    }

    /**
     * Acepta un pedido asignado.
     *
     * <p>Asocia al repartidor con el pedido y transiciona su estado a {@code ACCEPTED}.</p>
     *
     * @param orderId ID del pedido
     * @param request datos del repartidor que acepta
     * @return confirmación de aceptación del viaje
     */
    @PostMapping("/{orderId}/accept")
    @Operation(
            summary = "Aceptar viaje asignado",
            description = "Asigna al repartidor el pedido y cambia su estado a ACCEPTED."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Viaje aceptado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Estado del pedido inválido"),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado — requiere rol DRIVER")
    })
    public ResponseEntity<Map<String, Object>> acceptTrip(
            @Parameter(description = "ID del pedido a aceptar", required = true)
            @PathVariable("orderId") UUID orderId,
            @Valid @RequestBody AcceptTripRequest request) {

        driverDeliveryUseCase.acceptTrip(orderId, request.driverId());

        return ResponseEntity.ok(Map.of(
                "orderId", orderId,
                "driverId", request.driverId(),
                "status", "ACCEPTED",
                "message", "Viaje aceptado exitosamente. Pedido en estado ACCEPTED.",
                "acceptedAt", Instant.now().toString()
        ));
    }

    /**
     * Valida el PIN de entrega proporcionado por el cliente.
     *
     * <p>Si el PIN de 4 dígitos es correcto, transiciona a {@code DELIVERED}
     * y libera el saldo de la entrega al repartidor.</p>
     *
     * @param orderId ID del pedido
     * @param request PIN de 4 dígitos ingresado por el repartidor
     * @return confirmación de la entrega y liberación de fondos
     */
    @PostMapping("/{orderId}/validate-pin")
    @Operation(
            summary = "Validar PIN anti-fraude y confirmar entrega",
            description = "Valida el PIN de 4 dígitos. Si es exitoso, cambia el pedido a DELIVERED y libera el saldo de la orden."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PIN verificado y entrega confirmada con éxito"),
            @ApiResponse(responseCode = "400", description = "PIN incorrecto o formato inválido (máx 3 intentos)"),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado — requiere rol DRIVER")
    })
    public ResponseEntity<Map<String, Object>> validateDeliveryPin(
            @Parameter(description = "ID del pedido a confirmar", required = true)
            @PathVariable("orderId") UUID orderId,
            @Valid @RequestBody DriverValidatePinRequest request) {

        boolean confirmed = driverDeliveryUseCase.validateDeliveryPin(orderId, request.pin());

        return ResponseEntity.ok(Map.of(
                "orderId", orderId,
                "status", "DELIVERED",
                "confirmed", confirmed,
                "escrowReleased", true,
                "message", "PIN verificado con éxito. Pedido marcado como ENTREGADO y saldo liberado.",
                "deliveredAt", Instant.now().toString()
        ));
    }
}
