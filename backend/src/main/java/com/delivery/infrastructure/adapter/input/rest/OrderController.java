package com.delivery.infrastructure.adapter.input.rest;

import com.delivery.domain.model.Driver;
import com.delivery.domain.model.GeoLocation;
import com.delivery.domain.model.Order;
import com.delivery.domain.model.SettlementResult;
import com.delivery.domain.port.input.AssignNearestDriverUseCase;
import com.delivery.domain.port.input.CreateOrderUseCase;
import com.delivery.domain.port.input.ValidateDeliveryPinUseCase;
import com.delivery.domain.port.input.command.AssignNearestDriverCommand;
import com.delivery.domain.port.input.command.CreateOrderCommand;
import com.delivery.domain.port.input.command.ValidateDeliveryPinCommand;
import com.delivery.domain.port.output.FinancialSettlementPort;
import com.delivery.domain.port.output.OrderRepositoryPort;
import com.delivery.infrastructure.adapter.input.rest.dto.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de Entrada REST — Endpoints de Pedidos en Tulcán, Ecuador ($ USD).
 *
 * <p>Expone los casos de uso del dominio como endpoints HTTP RESTful.
 * Traduce DTOs ↔ Commands y delega la lógica al dominio.</p>
 *
 * <h3>Rutas soportadas:</h3>
 * {@code /api/v1/orders} y {@code /api/orders}
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping({"/v1/orders", "/orders"})
@Tag(name = "Pedidos", description = "Gestión del ciclo de vida de pedidos, creación y liquidación financiera")
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final ValidateDeliveryPinUseCase validateDeliveryPinUseCase;
    private final AssignNearestDriverUseCase assignNearestDriverUseCase;
    private final OrderRepositoryPort orderRepositoryPort;
    private final FinancialSettlementPort financialSettlementPort;

    public OrderController(CreateOrderUseCase createOrderUseCase,
                           ValidateDeliveryPinUseCase validateDeliveryPinUseCase,
                           AssignNearestDriverUseCase assignNearestDriverUseCase,
                           OrderRepositoryPort orderRepositoryPort,
                           FinancialSettlementPort financialSettlementPort) {
        this.createOrderUseCase = createOrderUseCase;
        this.validateDeliveryPinUseCase = validateDeliveryPinUseCase;
        this.assignNearestDriverUseCase = assignNearestDriverUseCase;
        this.orderRepositoryPort = orderRepositoryPort;
        this.financialSettlementPort = financialSettlementPort;
    }

    // ── Crear Pedido (Tulcán, Ecuador - $ USD) ──────────────────────

    @PostMapping
    @Operation(summary = "Crear un nuevo pedido en Tulcán",
               description = "Calcula coordenadas PostGIS en Tulcán, genera PIN anti-fraude, guarda el pedido en estado CREATED y retorna orderId.")
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {

        UUID custId = request.customerId() != null
                ? request.customerId()
                : UUID.fromString("00000000-0000-0000-0001-000000000001"); // Valeria Narváez Tulcán

        UUID merchId = request.merchantId() != null
                ? request.merchantId()
                : UUID.fromString("11111111-1111-1111-1111-111111111111"); // Asadero El Leñador Tulcán

        String pickupAddr = (request.pickupAddress() != null && !request.pickupAddress().isBlank())
                ? request.pickupAddress()
                : "Av. Sucre y Bolívar, Centro Comercial, Tulcán, Carchi";

        String deliveryAddr = (request.deliveryAddress() != null && !request.deliveryAddress().isBlank())
                ? request.deliveryAddress()
                : "Calle Brasil y Av. Tulcanaza, Sector Terminal Terrestre, Tulcán, Carchi";

        GeoLocation pickupLoc = new GeoLocation(request.effectivePickupLat(), request.effectivePickupLon());
        GeoLocation deliveryLoc = new GeoLocation(request.effectiveDeliveryLat(), request.effectiveDeliveryLon());

        BigDecimal total = request.calculateEffectiveTotal();

        CreateOrderCommand command = new CreateOrderCommand(
                custId,
                merchId,
                pickupAddr,
                pickupLoc,
                deliveryAddr,
                deliveryLoc,
                total,
                request.effectiveNotes()
        );

        Order order = createOrderUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(order));
    }

    // ── Consultar Pedido por ID ─────────────────────────────────────

    @GetMapping("/{orderId}")
    @Operation(summary = "Consultar pedido por ID", description = "Obtiene los detalles del pedido y su estado actual.")
    public ResponseEntity<?> getOrderById(@PathVariable UUID orderId) {
        return orderRepositoryPort.findById(orderId)
                .map(order -> ResponseEntity.ok(toResponse(order)))
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Consultar Pedido Más Reciente ───────────────────────────────

    @GetMapping("/latest")
    @Operation(summary = "Consultar pedido más reciente", description = "Obtiene el último pedido registrado en el sistema.")
    public ResponseEntity<?> getLatestOrder() {
        UUID defaultCustomer = UUID.fromString("00000000-0000-0000-0001-000000000001");
        List<Order> orders = orderRepositoryPort.findByCustomerId(defaultCustomer);
        if (orders.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(toResponse(orders.get(0)));
    }

    // ── Regenerar PIN de Entrega (Anti-Fraude) ─────────────────────

    @PostMapping("/{orderId}/regenerate-pin")
    @Operation(summary = "Regenerar PIN de entrega (anti-fraude)",
               description = "Genera un nuevo PIN OTP de 4 dígitos para el pedido especificado.")
    public ResponseEntity<?> regenerateDeliveryPin(@PathVariable UUID orderId) {
        return orderRepositoryPort.findById(orderId)
                .map(order -> {
                    order.regenerateDeliveryPin();
                    Order saved = orderRepositoryPort.save(order);
                    return ResponseEntity.ok(Map.of(
                            "orderId", saved.getId(),
                            "deliveryPin", saved.getDeliveryPin() != null ? saved.getDeliveryPin().getValue() : "",
                            "status", saved.getStatus().name(),
                            "message", "Nuevo código de verificación generado exitosamente"
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Validar PIN de Entrega (Anti-Fraude) ────────────────────────

    @PostMapping("/validate-pin")
    @Operation(summary = "Validar PIN de entrega (anti-fraude)",
               description = "Verifica el PIN OTP de 4 dígitos. Si es correcto, confirma la entrega y liquida fondos.")
    public ResponseEntity<Map<String, Object>> validateDeliveryPin(
            @Valid @RequestBody ValidateDeliveryPinRequest request) {

        ValidateDeliveryPinCommand command = new ValidateDeliveryPinCommand(
                request.orderId(), request.pin());

        boolean confirmed = validateDeliveryPinUseCase.execute(command);

        return ResponseEntity.ok(Map.of(
                "orderId", request.orderId(),
                "confirmed", confirmed,
                "message", confirmed
                        ? "Entrega confirmada exitosamente y fondos liquidados"
                        : "PIN incorrecto"
        ));
    }

    // ── Módulo de Pagos: Consultar Liquidación Financiera ───────────

    @GetMapping("/{orderId}/settlement")
    @Operation(summary = "Consultar liquidación financiera del pedido",
               description = "Retorna el desglose de liquidación: subtotal menos comisión al comercio y 100% de envío al repartidor.")
    public ResponseEntity<?> getSettlement(@PathVariable UUID orderId) {
        Optional<Order> orderOpt = orderRepositoryPort.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Order order = orderOpt.get();
        SettlementResult result = financialSettlementPort.settleOrder(order, null, null);

        return ResponseEntity.ok(Map.of(
                "orderId", result.orderId(),
                "currency", "USD",
                "totalAmount", result.totalAmount(),
                "shippingFee", result.shippingFee(),
                "subtotal", result.subtotal(),
                "commissionPercentage", result.commissionRate().multiply(BigDecimal.valueOf(100)) + "%",
                "platformCommission", result.platformCommission(),
                "merchantPayout", result.merchantPayout(),
                "driverPayout", result.driverPayout(),
                "settledAt", result.settledAt().toString()
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