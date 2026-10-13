package com.delivery.application.service;

import com.delivery.domain.enums.NotificationType;
import com.delivery.domain.enums.OrderStatus;
import com.delivery.domain.exception.InvalidDeliveryPinException;
import com.delivery.domain.exception.InvalidStateTransitionException;
import com.delivery.domain.exception.OrderNotFoundException;
import com.delivery.domain.model.Order;
import com.delivery.domain.port.input.DriverDeliveryUseCase;
import com.delivery.domain.port.output.DriverWalletPort;
import com.delivery.domain.port.output.NotificationPort;
import com.delivery.domain.port.output.OrderRepositoryPort;
import org.springframework.messaging.core.MessageSendingOperations;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Implementación del Caso de Uso para el Módulo del Repartidor.
 *
 * <p>Orquesta los flujos de:
 * <ul>
 *   <li>Aceptación del viaje: vinculación del repartidor y cambio a {@link OrderStatus#ACCEPTED}.</li>
 *   <li>Validación del PIN de entrega OTP (4 dígitos): cambio a {@link OrderStatus#DELIVERED},
 *       liberación del saldo mediante {@link DriverWalletPort} y notificación multicanal.</li>
 * </ul>
 * </p>
 *
 * @see DriverDeliveryUseCase
 * @since 1.0.0
 */
public class DriverDeliveryService implements DriverDeliveryUseCase {

    private static final Logger LOG = Logger.getLogger(DriverDeliveryService.class.getName());

    private final OrderRepositoryPort orderRepository;
    private final DriverWalletPort driverWalletPort;
    private final NotificationPort notificationPort;
    private final MessageSendingOperations<String> messagingTemplate;

    public DriverDeliveryService(OrderRepositoryPort orderRepository,
                                 DriverWalletPort driverWalletPort,
                                 NotificationPort notificationPort,
                                 MessageSendingOperations<String> messagingTemplate) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository no puede ser null");
        this.driverWalletPort = Objects.requireNonNull(driverWalletPort, "driverWalletPort no puede ser null");
        this.notificationPort = Objects.requireNonNull(notificationPort, "notificationPort no puede ser null");
        this.messagingTemplate = Objects.requireNonNull(messagingTemplate, "messagingTemplate no puede ser null");
    }

    @Override
    public void acceptTrip(UUID orderId, UUID driverId) {
        Objects.requireNonNull(orderId, "orderId no puede ser null");
        Objects.requireNonNull(driverId, "driverId no puede ser null");

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getStatus().isTerminal()) {
            throw new IllegalStateException(String.format(
                    "No es posible aceptar un pedido en estado terminal: %s (orderId=%s)",
                    order.getStatus(), orderId));
        }

        // Asignar el conductor al pedido
        order.assignDriver(driverId);

        // Si la orden está en CREATED, transicionar a ACCEPTED
        if (order.getStatus() == OrderStatus.CREATED) {
            order.transitionTo(OrderStatus.ACCEPTED, "Viaje aceptado por el repartidor " + driverId);
        }

        orderRepository.save(order);

        LOG.info(() -> String.format(
                "[DRIVER-TRIP] Repartidor %s aceptó la orden %s. Estado actual: %s",
                driverId, orderId, order.getStatus()));

        // Notificaciones de negocio
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", order.getId() != null ? order.getId() : orderId);
        payload.put("driverId", driverId);
        payload.put("status", order.getStatus().name());
        payload.put("timestamp", Instant.now().toString());

        notificationPort.notifyCustomer(order.getCustomerId(), NotificationType.DRIVER_ASSIGNED, payload);
        notificationPort.notifyMerchant(order.getMerchantId(), NotificationType.STATUS_UPDATED, payload);

        // Difusión WebSocket STOMP en tiempo real
        messagingTemplate.convertAndSend("/topic/orders/" + orderId + "/tracking", payload);
        messagingTemplate.convertAndSend("/topic/orders/" + orderId + "/status", payload);
    }

    @Override
    public boolean validateDeliveryPin(UUID orderId, String inputPin) {
        Objects.requireNonNull(orderId, "orderId no puede ser null");

        if (inputPin == null || !inputPin.matches("^\\d{4}$")) {
            throw new IllegalArgumentException("El PIN de entrega debe contener exactamente 4 dígitos numéricos");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // Para poder completar con PIN, el pedido debe estar en camino hacia el destino
        if (order.getStatus() == OrderStatus.READY_FOR_PICKUP) {
            order.transitionTo(OrderStatus.ON_THE_WAY, "Repartidor en ruta al destino de entrega");
        } else if (order.getStatus() != OrderStatus.ON_THE_WAY) {
            LOG.warning(() -> String.format(
                    "[DRIVER-PIN] Intento de validar PIN en estado inválido: %s para orden %s",
                    order.getStatus(), orderId));
            throw new InvalidStateTransitionException(order.getStatus(), OrderStatus.DELIVERED);
        }

        // Validación del PIN anti-fraude a través del modelo de dominio
        try {
            order.confirmDelivery(inputPin);
        } catch (InvalidDeliveryPinException ex) {
            // Guardar los intentos fallidos para activar bloqueo por fuerza bruta tras 3 fallos
            orderRepository.save(order);
            LOG.warning(() -> String.format(
                    "[DRIVER-PIN] PIN incorrecto para orden %s. Intentos restantes: %d",
                    orderId,
                    order.getDeliveryPin() != null ? order.getDeliveryPin().getRemainingAttempts() : 0));
            throw ex;
        }

        // Persistir el estado DELIVERED
        orderRepository.save(order);

        // REGLA CLAVE: Liberar el saldo/fondos en custodia al repartidor
        if (order.getDriverId() != null) {
            driverWalletPort.releaseEscrowFunds(orderId, order.getDriverId(), order.getTotalAmount());
        }

        LOG.info(() -> String.format(
                "[DRIVER-PIN] Entrega confirmada y saldo liberado. Orden: %s, Repartidor: %s, Monto: %s",
                orderId, order.getDriverId(), order.getTotalAmount()));

        Map<String, Object> deliveryPayload = new HashMap<>();
        deliveryPayload.put("orderId", order.getId() != null ? order.getId() : orderId);
        deliveryPayload.put("driverId", order.getDriverId());
        deliveryPayload.put("status", OrderStatus.DELIVERED.name());
        deliveryPayload.put("totalAmount", order.getTotalAmount());
        deliveryPayload.put("deliveredAt", Instant.now().toString());

        // Notificaciones
        notificationPort.notifyCustomer(order.getCustomerId(), NotificationType.DELIVERY_CONFIRMED, deliveryPayload);
        if (order.getDriverId() != null) {
            notificationPort.notifyDriver(order.getDriverId(), NotificationType.DELIVERY_CONFIRMED, deliveryPayload);
        }

        // Notificar en tiempo real por WebSocket
        messagingTemplate.convertAndSend("/topic/orders/" + orderId + "/tracking", deliveryPayload);
        messagingTemplate.convertAndSend("/topic/orders/" + orderId + "/status", deliveryPayload);

        return true;
    }
}
