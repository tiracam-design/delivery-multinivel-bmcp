package com.delivery.infrastructure.adapter.input.rest;

import com.delivery.application.dto.OrderResponseDTO;
import com.delivery.domain.model.GeoLocation;
import com.delivery.domain.port.input.ClientOrderUseCase;
import com.delivery.domain.port.input.command.CreateOrderCommand;
import com.delivery.infrastructure.adapter.input.rest.dto.CreateOrderRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Adaptador de Entrada REST — Módulo del Cliente.
 *
 * <p>Expone los endpoints HTTP para la gestión de pedidos por parte de clientes
 * autenticados. Controla la seguridad de acceso mediante roles ({@code ROLE_CLIENT}),
 * valida las solicitudes entrantes y delega en el puerto {@link ClientOrderUseCase}.</p>
 *
 * <h3>Ruta base:</h3>
 * {@code /api/v1/client/orders}
 *
 * @see ClientOrderUseCase
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/client/orders")
@PreAuthorize("hasRole('CLIENT')")
@Tag(name = "Módulo Cliente - Pedidos", description = "Endpoints para creación y consulta de pedidos del cliente")
@SecurityRequirement(name = "bearerAuth")
public class ClientOrderController {

    private final ClientOrderUseCase clientOrderUseCase;

    public ClientOrderController(ClientOrderUseCase clientOrderUseCase) {
        this.clientOrderUseCase = Objects.requireNonNull(clientOrderUseCase,
                "clientOrderUseCase no puede ser null");
    }

    /**
     * Endpoint para crear un nuevo pedido de cliente.
     *
     * <p>Calcula el costo de entrega mediante PostGIS, genera un PIN de seguridad
     * de 4 dígitos, persiste el pedido en estado CREATED y difunde la notificación
     * al comercio vía WebSocket.</p>
     *
     * @param request datos del pedido validados por Jakarta Validation
     * @return pedido creado con ID asignado, PIN y costo de envío calculado
     */
    @PostMapping
    @Operation(
            summary = "Crear un nuevo pedido",
            description = "Calcula el costo de envío con PostGIS, genera PIN anti-fraude (OTP 4 dígitos), "
                    + "guarda el pedido en estado CREATED y notifica al comercio vía WebSocket."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pedido creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o coordenadas fuera de rango"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado — requiere rol CLIENT")
    })
    public ResponseEntity<OrderResponseDTO> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {

        GeoLocation pickupLocation = (request.pickupLatitude() != null && request.pickupLongitude() != null)
                ? new GeoLocation(request.pickupLatitude(), request.pickupLongitude())
                : null;

        GeoLocation deliveryLocation = (request.deliveryLatitude() != null && request.deliveryLongitude() != null)
                ? new GeoLocation(request.deliveryLatitude(), request.deliveryLongitude())
                : null;

        CreateOrderCommand command = new CreateOrderCommand(
                request.customerId(),
                request.merchantId(),
                request.pickupAddress(),
                pickupLocation,
                request.deliveryAddress(),
                deliveryLocation,
                request.totalAmount(),
                request.notes()
        );

        OrderResponseDTO createdOrder = clientOrderUseCase.createOrder(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
    }

    /**
     * Consulta los pedidos activos del cliente autenticado o especificado por parámetro.
     *
     * @param clientId ID opcional del cliente (si no se proporciona se puede resolver por autenticación)
     * @param authentication contexto de seguridad de Spring
     * @return lista de pedidos con estado no terminal
     */
    @GetMapping("/active")
    @Operation(
            summary = "Consultar pedidos activos del cliente",
            description = "Retorna los pedidos activos (CREATED, ACCEPTED, IN_PREPARATION, READY_FOR_PICKUP, ON_THE_WAY, INCIDENT_PAUSED)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de pedidos activos obtenida correctamente"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado — requiere rol CLIENT")
    })
    public ResponseEntity<List<OrderResponseDTO>> getActiveOrders(
            @Parameter(description = "ID del cliente (UUID)")
            @RequestParam(name = "clientId", required = false) UUID clientId,
            Authentication authentication) {

        UUID resolvedClientId = resolveClientId(clientId, authentication);
        List<OrderResponseDTO> activeOrders = clientOrderUseCase.getClientActiveOrders(resolvedClientId);
        return ResponseEntity.ok(activeOrders);
    }

    /**
     * Consulta los pedidos activos especificando el ID en la ruta.
     *
     * @param clientId ID único del cliente
     * @return lista de pedidos activos
     */
    @GetMapping("/active/{clientId}")
    @Operation(
            summary = "Consultar pedidos activos por ID de cliente",
            description = "Retorna la lista de pedidos en curso pertenecientes al cliente indicado"
    )
    public ResponseEntity<List<OrderResponseDTO>> getActiveOrdersByPath(
            @Parameter(description = "ID del cliente (UUID)", required = true)
            @PathVariable("clientId") UUID clientId) {

        List<OrderResponseDTO> activeOrders = clientOrderUseCase.getClientActiveOrders(clientId);
        return ResponseEntity.ok(activeOrders);
    }

    // ════════════════════════════════════════════════════════════════
    //  MÉTODOS PRIVADOS AUXILIARES
    // ════════════════════════════════════════════════════════════════

    /**
     * Resuelve el identificador del cliente a partir del parámetro de query
     * o del {@link Authentication} de la sesión actual.
     */
    private UUID resolveClientId(UUID queryClientId, Authentication authentication) {
        if (queryClientId != null) {
            return queryClientId;
        }

        if (authentication != null && authentication.getName() != null) {
            try {
                return UUID.fromString(authentication.getName());
            } catch (IllegalArgumentException ignored) {
                // El principal puede ser username/email en vez de UUID
            }
        }

        throw new IllegalArgumentException(
                "No se pudo determinar el clientId del cliente. Envíe el parámetro 'clientId'.");
    }
}
