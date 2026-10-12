package com.delivery.infrastructure.adapter.input.rest;

import com.delivery.domain.model.Driver;
import com.delivery.domain.model.GeoLocation;
import com.delivery.domain.model.Order;
import com.delivery.domain.port.input.AssignNearestDriverUseCase;
import com.delivery.domain.port.input.CreateOrderUseCase;
import com.delivery.domain.port.input.ValidateDeliveryPinUseCase;
import com.delivery.domain.port.input.command.AssignNearestDriverCommand;
import com.delivery.domain.port.input.command.CreateOrderCommand;
import com.delivery.domain.port.input.command.ValidateDeliveryPinCommand;
import com.delivery.infrastructure.adapter.input.rest.dto.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * Adaptador de Entrada REST — Endpoints de Pedidos.
 *
 * <p>Expone los casos de uso del dominio como endpoints HTTP RESTful.
 * Traduce DTOs ↔ Commands y delega la lógica al dominio.</p>
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/orders")
@Tag(name = "Pedidos", description = "Gestión del ciclo de vida de pedidos")
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final ValidateDeliveryPinUseCase validateDeliveryPinUseCase;
    private final AssignNearestDriverUseCase assignNearestDriverUseCase;

    public OrderController(CreateOrderUseCase createOrderUseCase,
                           ValidateDeliveryPinUseCase validateDeliveryPinUseCase,
                           AssignNearestDriverUseCase assignNearestDriverUseCase) {
        this.createOrderUseCase = createOrderUseCase;
        this.validateDeliveryPinUseCase = validateDeliveryPinUseCase;
        this.assignNearestDriverUseCase = assignNearestDriverUseCase;
    }

    // ── Crear Pedido ────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Crear un nuevo pedido",
               description = "Crea un pedido en estado CREATED con PIN anti-fraude generado automáticamente")
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {

        GeoLocation pickupLoc = buildGeoLocation(
                request.pickupLatitude(), request.pickupLongitude());
        GeoLocation deliveryLoc = buildGeoLocation(
                request.deliveryLatitude(), request.deliveryLongitude());

        CreateOrderCommand command = new CreateOrderCommand(
                request.customerId(),
                request.merchantId(),
                request.pickupAddress(),
                pickupLoc,
                request.deliveryAddress(),
                deliveryLoc,
                request.totalAmount(),
                request.notes()
        );

        Order order = createOrderUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(order));
    }

    // ── Validar PIN de Entrega ──────────────────────────────────────

    @PostMapping("/validate-pin")
    @Operation(summary = "Validar PIN de entrega (anti-fraude)",
               description = "Verifica el PIN OTP de 4 dígitos. Si es correcto, confirma la entrega.")
    public ResponseEntity<Map<String, Object>> validateDeliveryPin(
            @Valid @RequestBody ValidateDeliveryPinRequest request) {

        ValidateDeliveryPinCommand command = new ValidateDeliveryPinCommand(
                request.orderId(), request.pin());

        boolean confirmed = validateDeliveryPinUseCase.execute(command);

        return ResponseEntity.ok(Map.of(
                "orderId", request.orderId(),
                "confirmed", confirmed,
                "message", confirmed
                        ? "Entrega confirmada exitosamente"
                        : "PIN incorrecto"
        ));
    }

    // ── Asignar Repartidor Más Cercano ──────────────────────────────

    @PostMapping("/assign-driver")
    @Operation(summary = "Asignar repartidor más cercano",
               description = "Busca repartidores disponibles dentro del radio y asigna al de mayor prioridad multinivel")
    public ResponseEntity<?> assignNearestDriver(
            @Valid @RequestBody AssignDriverRequest request) {

        AssignNearestDriverCommand command = new AssignNearestDriverCommand(
                request.orderId(), request.getRadiusKmOrDefault());

        Optional<Driver> driver = assignNearestDriverUseCase.execute(command);

        if (driver.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "orderId", request.orderId(),
                    "assigned", false,
                    "message", "No se encontraron repartidores disponibles dentro del radio"
            ));
        }

        Driver d = driver.get();
        return ResponseEntity.ok(Map.of(
                "orderId", request.orderId(),
                "assigned", true,
                "driver", new DriverResponse(
                        d.getId(),
                        d.getFullName(),
                        d.getPhone(),
                        d.getLevel().name(),
                        d.getDistanceKm()
                )
        ));
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private GeoLocation buildGeoLocation(Double lat, Double lon) {
        if (lat != null && lon != null) {
            return new GeoLocation(lat, lon);
        }
        return null;
    }

    private OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getMerchantId(),
                order.getDriverId(),
                order.getStatus().name(),
                order.getPickupAddress(),
                order.getDeliveryAddress(),
                order.getEstimatedDistanceKm(),
                order.getEstimatedDurationMinutes(),
                order.getTotalAmount(),
                order.getDeliveryPin() != null
                        ? order.getDeliveryPin().getValue()
                        : null,
                order.getNotes(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
