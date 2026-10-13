package com.delivery.application.service;

import com.delivery.application.dto.OrderResponseDTO;
import com.delivery.domain.model.DeliveryPin;
import com.delivery.domain.model.GeoLocation;
import com.delivery.domain.model.Order;
import com.delivery.domain.port.input.ClientOrderUseCase;
import com.delivery.domain.port.input.command.CreateOrderCommand;
import com.delivery.domain.port.output.OrderRepositoryPort;

import org.springframework.messaging.simp.SimpMessageSendingOperations;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Implementación del caso de uso del Módulo del Cliente: {@link ClientOrderUseCase}.
 *
 * <p>Responsabilidades principales:</p>
 * <ol>
 *   <li>Cálculo geoespacial de costos de envío basado en coordenadas PostGIS WGS 84.</li>
 *   <li>Generación del código PIN anti-fraude aleatorio de 4 dígitos ({@link DeliveryPin}).</li>
 *   <li>Persistencia del pedido en estado inicial {@code CREATED}.</li>
 *   <li>Notificación en tiempo real al comercio mediante WebSocket
 *       al topic {@code /topic/merchant/{merchantId}/orders}.</li>
 *   <li>Consulta y filtrado de pedidos activos del cliente.</li>
 * </ol>
 *
 * @see ClientOrderUseCase
 * @since 1.0.0
 */
public class ClientOrderService implements ClientOrderUseCase {

    private static final Logger LOG = Logger.getLogger(ClientOrderService.class.getName());

    /** Tarifa base de envío en unidades monetarias */
    public static final BigDecimal BASE_SHIPPING_FEE = new BigDecimal("2.50");

    /** Tarifa por kilómetro recorrido */
    public static final BigDecimal RATE_PER_KM = new BigDecimal("1.20");

    /** Costo mínimo garantizado de envío */
    public static final BigDecimal MIN_SHIPPING_FEE = new BigDecimal("3.00");

    /** Prefijo para el canal WebSocket del comercio */
    private static final String MERCHANT_ORDERS_TOPIC_PREFIX = "/topic/merchant/";
    private static final String ORDERS_TOPIC_SUFFIX = "/orders";

    private final OrderRepositoryPort orderRepository;
    private final SimpMessageSendingOperations messagingTemplate;

    public ClientOrderService(OrderRepositoryPort orderRepository,
                              SimpMessageSendingOperations messagingTemplate) {
        this.orderRepository = Objects.requireNonNull(orderRepository,
                "orderRepository no puede ser null");
        this.messagingTemplate = Objects.requireNonNull(messagingTemplate,
                "messagingTemplate no puede ser null");
    }

    @Override
    public OrderResponseDTO createOrder(CreateOrderCommand command) {
        Objects.requireNonNull(command, "command no puede ser null");

        LOG.info(() -> String.format(
                "[CLIENT-ORDER] Iniciando creación de pedido para cliente=%s en comercio=%s",
                command.getCustomerId(), command.getMerchantId()));

        // ── Paso 1: Cálculo del costo de envío con PostGIS ───────────
        Double distanceKm = calculateDistanceWithPostGIS(
                command.getPickupLocation(),
                command.getDeliveryLocation()
        );

        BigDecimal shippingCost = calculateShippingCost(distanceKm);
        BigDecimal finalTotalAmount = command.getTotalAmount().add(shippingCost);

        LOG.info(() -> String.format(
                "[CLIENT-ORDER] Distancia PostGIS: %s km | Tarifa envío: %s | Total con envío: %s",
                distanceKm != null ? String.format("%.2f", distanceKm) : "N/A",
                shippingCost, finalTotalAmount));

        // ── Paso 2 y 3: Crear entidad en estado CREATED con PIN 4 dígitos ─
        // Order.create genera internamente un DeliveryPin aleatorio de 4 dígitos
        // con SecureRandom e inicializa el pedido en OrderStatus.CREATED
        Order order = Order.create(
                command.getCustomerId(),
                command.getMerchantId(),
                command.getPickupAddress(),
                command.getPickupLocation(),
                command.getDeliveryAddress(),
                command.getDeliveryLocation(),
                finalTotalAmount,
                command.getNotes()
        );

        // ── Paso 4: Persistir el pedido en la base de datos ──────────
        Order savedOrder = orderRepository.save(order);

        LOG.info(() -> String.format(
                "[CLIENT-ORDER] Pedido persistido exitosamente. orderId=%s, status=%s, PIN=%s",
                savedOrder.getId(), savedOrder.getStatus(),
                savedOrder.getDeliveryPin() != null ? savedOrder.getDeliveryPin().getValue() : "N/A"));

        // ── Paso 5: Mapear a DTO de respuesta ────────────────────────
        OrderResponseDTO responseDTO = OrderResponseDTO.fromDomain(savedOrder, shippingCost);

        // ── Paso 6: Notificar al comercio vía WebSocket ───────────────
        String destinationTopic = MERCHANT_ORDERS_TOPIC_PREFIX
                + savedOrder.getMerchantId()
                + ORDERS_TOPIC_SUFFIX;

        notifyMerchantViaWebSocket(destinationTopic, responseDTO);

        return responseDTO;
    }

    @Override
    public List<OrderResponseDTO> getClientActiveOrders(UUID clientId) {
        Objects.requireNonNull(clientId, "clientId no puede ser null");

        LOG.fine(() -> String.format(
                "[CLIENT-ORDER] Consultando pedidos activos para cliente=%s", clientId));

        List<Order> clientOrders = orderRepository.findByCustomerId(clientId);

        return clientOrders.stream()
                .filter(Order::isActive)
                .map(order -> {
                    BigDecimal shipping = calculateShippingCost(order.getEstimatedDistanceKm());
                    return OrderResponseDTO.fromDomain(order, shipping);
                })
                .toList();
    }

    // ════════════════════════════════════════════════════════════════
    //  MÉTODOS AUXILIARES DE NEGOCIO
    // ════════════════════════════════════════════════════════════════

    /**
     * Calcula la distancia geodésica entre dos puntos usando el puerto PostGIS
     * con fallback a Haversine en el dominio si es necesario.
     */
    private Double calculateDistanceWithPostGIS(GeoLocation pickup, GeoLocation delivery) {
        if (pickup == null || delivery == null) {
            return null;
        }

        try {
            Double distance = orderRepository.calculateDistanceKm(pickup, delivery);
            if (distance != null) {
                return distance;
            }
        } catch (Exception ex) {
            LOG.warning(() -> "[CLIENT-ORDER] Error calculando distancia PostGIS: "
                    + ex.getMessage() + ". Utilizando cálculo de dominio.");
        }

        return pickup.distanceKmTo(delivery);
    }

    /**
     * Calcula la tarifa de envío según la distancia geodésica PostGIS.
     *
     * <p>Fórmula: {@code MAX(MIN_SHIPPING_FEE, BASE_FEE + (distanciaKm * RATE_PER_KM))}</p>
     */
    public BigDecimal calculateShippingCost(Double distanceKm) {
        if (distanceKm == null || distanceKm <= 0.0) {
            return MIN_SHIPPING_FEE;
        }

        BigDecimal variableFee = RATE_PER_KM.multiply(BigDecimal.valueOf(distanceKm));
        BigDecimal computedFee = BASE_SHIPPING_FEE.add(variableFee);

        return computedFee.max(MIN_SHIPPING_FEE).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Publica el nuevo pedido en el broker WebSocket STOMP del comercio.
     */
    private void notifyMerchantViaWebSocket(String destination, OrderResponseDTO payload) {
        try {
            messagingTemplate.convertAndSend(destination, payload);
            LOG.info(() -> String.format(
                    "📡 [WEBSOCKET] Notificación de nuevo pedido enviada a %s (orderId=%s)",
                    destination, payload.id()));
        } catch (Exception ex) {
            LOG.severe(() -> String.format(
                    "⚠️ [WEBSOCKET] Fallo al notificar comercio en %s: %s",
                    destination, ex.getMessage()));
        }
    }
}
