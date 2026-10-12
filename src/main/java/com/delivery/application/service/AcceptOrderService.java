package com.delivery.application.service;

import com.delivery.domain.enums.NotificationType;
import com.delivery.domain.enums.OrderStatus;
import com.delivery.domain.exception.OrderNotFoundException;
import com.delivery.domain.model.Driver;
import com.delivery.domain.model.Order;
import com.delivery.domain.port.input.AcceptOrderUseCase;
import com.delivery.domain.port.input.AssignNearestDriverUseCase;
import com.delivery.domain.port.input.command.AssignNearestDriverCommand;
import com.delivery.domain.port.output.DriverGeolocationPort;
import com.delivery.domain.port.output.NotificationPort;
import com.delivery.domain.port.output.OrderRepositoryPort;

import org.springframework.messaging.simp.SimpMessageSendingOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Implementación del caso de uso: Aceptar Pedido por el Comercio.
 *
 * <p>Flujo de ejecución:</p>
 * <ol>
 *   <li>Recupera y valida la existencia del pedido.</li>
 *   <li>Aplica las transiciones a {@link OrderStatus#ACCEPTED} y {@link OrderStatus#IN_PREPARATION}.</li>
 *   <li>Establece los minutos estimados de preparación.</li>
 *   <li>Persiste el estado actualizado en el repositorio.</li>
 *   <li>Inicia la búsqueda espacial de repartidores a &lt; 3 km usando PostGIS ({@code ST_DWithin}).</li>
 *   <li>Notifica al cliente en tiempo real vía WebSocket con la hora estimada de entrega.</li>
 * </ol>
 *
 * @see AcceptOrderUseCase
 * @since 1.0.0
 */
public class AcceptOrderService implements AcceptOrderUseCase {

    private static final Logger LOG = Logger.getLogger(AcceptOrderService.class.getName());

    /** Radio máximo para la búsqueda geoespacial PostGIS de repartidores cercanos */
    public static final double DRIVER_SEARCH_RADIUS_KM = 3.0;

    private final OrderRepositoryPort orderRepository;
    private final DriverGeolocationPort driverGeolocationPort;
    private final AssignNearestDriverUseCase assignNearestDriverUseCase;
    private final SimpMessageSendingOperations messagingTemplate;
    private final NotificationPort notificationPort;

    public AcceptOrderService(OrderRepositoryPort orderRepository,
                              DriverGeolocationPort driverGeolocationPort,
                              AssignNearestDriverUseCase assignNearestDriverUseCase,
                              SimpMessageSendingOperations messagingTemplate,
                              NotificationPort notificationPort) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository no puede ser null");
        this.driverGeolocationPort = Objects.requireNonNull(driverGeolocationPort, "driverGeolocationPort no puede ser null");
        this.assignNearestDriverUseCase = assignNearestDriverUseCase;
        this.messagingTemplate = Objects.requireNonNull(messagingTemplate, "messagingTemplate no puede ser null");
        this.notificationPort = Objects.requireNonNull(notificationPort, "notificationPort no puede ser null");
    }

    @Override
    public void acceptOrder(UUID orderId, int preparationMinutes) {
        Objects.requireNonNull(orderId, "orderId es obligatorio");
        if (preparationMinutes <= 0) {
            throw new IllegalArgumentException("preparationMinutes debe ser mayor a 0, recibido: " + preparationMinutes);
        }

        LOG.info(() -> String.format(
                "[MERCHANT] Aceptando pedido orderId=%s con tiempo de preparación=%d min",
                orderId, preparationMinutes));

        // ── Paso 1: Recuperar el pedido ──────────────────────────────
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // ── Paso 2: Transición a IN_PREPARATION y registro de tiempo ─
        if (order.getStatus() == OrderStatus.CREATED) {
            order.transitionTo(OrderStatus.ACCEPTED, "Comercio aceptó el pedido");
        }
        order.transitionTo(OrderStatus.IN_PREPARATION,
                String.format("Comercio inició preparación (%d minutos estimados)", preparationMinutes));
        order.setPreparationTime(preparationMinutes);

        // ── Paso 3: Persistir cambios ────────────────────────────────
        orderRepository.save(order);

        LOG.info(() -> String.format(
                "[MERCHANT] Pedido orderId=%s actualizado a estado IN_PREPARATION", orderId));

        // ── Paso 4: Búsqueda PostGIS de repartidores a < 3 km ─────────
        initiateDriverSearch(order);

        // ── Paso 5: Notificación WebSocket al cliente ────────────────
        notifyCustomerOrderInPreparation(order, preparationMinutes);
    }

    /**
     * Inicia la búsqueda geoespacial PostGIS de repartidores a menos de 3 km.
     */
    private void initiateDriverSearch(Order order) {
        if (order.getPickupLocation() == null) {
            LOG.warning(() -> String.format(
                    "[MERCHANT-GEO] Pedido %s no tiene pickupLocation. Omitiendo búsqueda PostGIS.",
                    order.getId()));
            return;
        }

        LOG.info(() -> String.format(
                "[MERCHANT-GEO] Ejecutando búsqueda espacial PostGIS para repartidores a < %.1f km "
                        + "desde ubicación de recogida [lat=%.6f, lon=%.6f]",
                DRIVER_SEARCH_RADIUS_KM,
                order.getPickupLocation().getLatitude(),
                order.getPickupLocation().getLongitude()));

        List<Driver> candidateDrivers = driverGeolocationPort.findAvailableDriversWithinRadius(
                order.getPickupLocation(), DRIVER_SEARCH_RADIUS_KM);

        LOG.info(() -> String.format(
                "[MERCHANT-GEO] Encontrados %d repartidores disponibles a < %.1f km para pedido %s",
                candidateDrivers.size(), DRIVER_SEARCH_RADIUS_KM, order.getId()));

        // Si hay caso de uso de asignación configurado, se orquesta la asignación automática
        if (assignNearestDriverUseCase != null && !candidateDrivers.isEmpty() && !order.hasDriverAssigned()) {
            try {
                Optional<Driver> assigned = assignNearestDriverUseCase.execute(
                        new AssignNearestDriverCommand(order.getId(), DRIVER_SEARCH_RADIUS_KM));
                assigned.ifPresent(driver -> LOG.info(() -> String.format(
                        "[MERCHANT-GEO] Repartidor asignado exitosamente: %s (id=%s, dist=%.2f km)",
                        driver.getFullName(), driver.getId(), driver.getDistanceKm())));
            } catch (Exception ex) {
                LOG.warning(() -> "[MERCHANT-GEO] Error al auto-asignar repartidor: " + ex.getMessage());
            }
        }
    }

    /**
     * Notifica al cliente vía WebSocket la hora estimada de entrega y estado de preparación.
     */
    private void notifyCustomerOrderInPreparation(Order order, int preparationMinutes) {
        UUID effectiveOrderId = order.getId() != null ? order.getId() : order.getCustomerId();
        if (order.getId() != null) {
            effectiveOrderId = order.getId();
        }

        Instant now = Instant.now();
        Instant estimatedDeliveryTime = now.plus(Duration.ofMinutes(preparationMinutes));

        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("orderId", order.getId() != null ? order.getId() : order.getCustomerId());
        payload.put("status", OrderStatus.IN_PREPARATION.name());
        payload.put("message", "Tu pedido está siendo preparado por el comercio");
        payload.put("preparationMinutes", preparationMinutes);
        payload.put("estimatedDeliveryTime", estimatedDeliveryTime.toString());
        payload.put("updatedAt", now.toString());

        // Difusión en topic de la orden para el cliente
        String targetId = order.getId() != null ? order.getId().toString() : "current";
        String orderTrackingTopic = "/topic/orders/" + targetId + "/tracking";
        String orderStatusTopic = "/topic/orders/" + targetId + "/status";

        try {
            messagingTemplate.convertAndSend(orderTrackingTopic, payload);
            messagingTemplate.convertAndSend(orderStatusTopic, payload);
            LOG.info(() -> String.format(
                    "📡 [WEBSOCKET] Notificación enviada a %s y %s (ETA: %s)",
                    orderTrackingTopic, orderStatusTopic, estimatedDeliveryTime));
        } catch (Exception ex) {
            LOG.severe(() -> "⚠️ [WEBSOCKET] Fallo al enviar notificación WebSocket: " + ex.getMessage());
        }

        // Notificación de dominio
        notificationPort.notifyCustomer(
                order.getCustomerId(),
                NotificationType.STATUS_UPDATED,
                payload
        );
    }
}
