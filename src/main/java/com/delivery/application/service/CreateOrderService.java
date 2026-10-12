package com.delivery.application.service;

import com.delivery.domain.enums.NotificationType;
import com.delivery.domain.model.Order;
import com.delivery.domain.port.input.CreateOrderUseCase;
import com.delivery.domain.port.input.command.CreateOrderCommand;
import com.delivery.domain.port.output.NotificationPort;
import com.delivery.domain.port.output.OrderRepositoryPort;

import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Implementación del caso de uso: Crear Pedido.
 *
 * <p>Orquesta la creación de un nuevo pedido, generación del PIN
 * anti-fraude, persistencia y notificación al comercio.</p>
 *
 * @see CreateOrderUseCase
 * @since 1.0.0
 */
public class CreateOrderService implements CreateOrderUseCase {

    private static final Logger LOG = Logger.getLogger(
            CreateOrderService.class.getName());

    private final OrderRepositoryPort orderRepository;
    private final NotificationPort notificationPort;

    public CreateOrderService(OrderRepositoryPort orderRepository,
                              NotificationPort notificationPort) {
        this.orderRepository = Objects.requireNonNull(orderRepository,
                "orderRepository no puede ser null");
        this.notificationPort = Objects.requireNonNull(notificationPort,
                "notificationPort no puede ser null");
    }

    @Override
    public Order execute(CreateOrderCommand command) {
        Objects.requireNonNull(command, "command no puede ser null");

        // ── Paso 1: Crear el pedido con PIN anti-fraude ─────────────
        Order order = Order.create(
                command.getCustomerId(),
                command.getMerchantId(),
                command.getPickupAddress(),
                command.getPickupLocation(),
                command.getDeliveryAddress(),
                command.getDeliveryLocation(),
                command.getTotalAmount(),
                command.getNotes()
        );

        // ── Paso 2: Persistir ───────────────────────────────────────
        Order savedOrder = orderRepository.save(order);

        LOG.info(() -> String.format(
                "[ORDER] Pedido creado exitosamente. orderId=%s, customerId=%s, "
                        + "merchantId=%s, amount=%s",
                savedOrder.getId(), savedOrder.getCustomerId(),
                savedOrder.getMerchantId(), savedOrder.getTotalAmount()));

        // ── Paso 3: Notificar al comercio ───────────────────────────
        notificationPort.notifyMerchant(
                savedOrder.getMerchantId(),
                NotificationType.ORDER_CREATED,
                Map.of(
                        "orderId", savedOrder.getId(),
                        "customerId", savedOrder.getCustomerId(),
                        "pickupAddress", savedOrder.getPickupAddress(),
                        "totalAmount", savedOrder.getTotalAmount()
                )
        );

        // ── Paso 4: Notificar al cliente con el PIN ─────────────────
        notificationPort.notifyCustomer(
                savedOrder.getCustomerId(),
                NotificationType.DELIVERY_PIN_GENERATED,
                Map.of(
                        "orderId", savedOrder.getId(),
                        "pin", savedOrder.getDeliveryPin().getValue(),
                        "expiresAt", savedOrder.getDeliveryPin().getExpiresAt().toString()
                )
        );

        return savedOrder;
    }
}
