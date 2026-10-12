package com.delivery.infrastructure.adapter.output.redis.listener;

import com.delivery.domain.enums.NotificationType;
import com.delivery.domain.enums.OrderStatus;
import com.delivery.domain.model.Order;
import com.delivery.domain.port.output.NotificationPort;
import com.delivery.domain.port.output.OrderRepositoryPort;
import com.delivery.infrastructure.adapter.output.redis.RedisOrderExpirationAdapter;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listener de Redis Keyspace Notifications para expiración de pedidos no aceptados.
 *
 * <p>Escucha el topic {@code __keyevent@*__:expired}.
 * Cuando expira una clave con prefijo {@code order:unaccepted:{orderId}} (tras 5 minutos):
 * <ul>
 *   <li>Recupera el pedido desde la persistencia.</li>
 *   <li>Si continúa en estado inicial (ej. {@code CREATED}), se cancela por inacción del restaurante.</li>
 *   <li>Se dispara la notificación al cliente (para reembolso) y al restaurante (aviso de penalización/timeout).</li>
 * </ul>
 * </p>
 *
 * @since 1.0.0
 */
@Component
public class OrderUnacceptedExpirationListener implements MessageListener {

    private static final Logger LOG = Logger.getLogger(
            OrderUnacceptedExpirationListener.class.getName());

    private final OrderRepositoryPort orderRepositoryPort;
    private final NotificationPort notificationPort;

    public OrderUnacceptedExpirationListener(OrderRepositoryPort orderRepositoryPort,
                                             NotificationPort notificationPort) {
        this.orderRepositoryPort = Objects.requireNonNull(orderRepositoryPort,
                "orderRepositoryPort no puede ser null");
        this.notificationPort = Objects.requireNonNull(notificationPort,
                "notificationPort no puede ser null");
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        byte[] body = message.getBody();
        if (body == null || body.length == 0) {
            return;
        }

        String expiredKey = new String(body, StandardCharsets.UTF_8);

        // Filtrar exclusivamente claves de pedidos no aceptados
        if (expiredKey.startsWith(RedisOrderExpirationAdapter.KEY_PREFIX)) {
            handleOrderUnacceptedExpiration(expiredKey);
        }
    }

    private void handleOrderUnacceptedExpiration(String expiredKey) {
        String orderIdStr = expiredKey.substring(RedisOrderExpirationAdapter.KEY_PREFIX.length());
        LOG.warning(() -> String.format(
                "[REDIS-KEYSPACE-NOTIFICATION] Evento de expiración detectado para key=%s (orderId=%s)",
                expiredKey, orderIdStr));

        UUID orderId;
        try {
            orderId = UUID.fromString(orderIdStr);
        } catch (IllegalArgumentException ex) {
            LOG.severe(() -> "[REDIS-KEYSPACE] Formato UUID inválido en clave expirada: " + expiredKey);
            return;
        }

        try {
            Optional<Order> orderOpt = orderRepositoryPort.findById(orderId);
            if (orderOpt.isEmpty()) {
                LOG.warning(() -> "[REDIS-KEYSPACE] Pedido no encontrado en DB para orderId=" + orderId);
                return;
            }

            Order order = orderOpt.get();

            // Si el pedido aún no fue aceptado ni preparado por el restaurante
            if (order.getStatus() == OrderStatus.CREATED) {
                LOG.info(() -> String.format(
                        "[TIMEOUT-RESTAURANTE] Pedido %s expiró tras 5 minutos sin aceptación. Cancelando pedido...",
                        orderId));

                // Transición a CANCELLED por inacción del restaurante
                order.transitionTo(OrderStatus.CANCELLED,
                        "Expiración de tiempo límite (5 min): El restaurante no confirmó el pedido.");
                orderRepositoryPort.save(order);

                // Notificar al cliente para liberación de fondos o disculpa
                notificationPort.notifyCustomer(
                        order.getCustomerId(),
                        NotificationType.ORDER_CANCELLED,
                        Map.of(
                                "orderId", order.getId(),
                                "reason", "El restaurante no confirmó su pedido en el tiempo límite (5 minutos). Su dinero ha sido reembolsado.",
                                "status", OrderStatus.CANCELLED.name()
                        )
                );

                // Notificar al restaurante
                notificationPort.notifyMerchant(
                        order.getMerchantId(),
                        NotificationType.ORDER_CANCELLED,
                        Map.of(
                                "orderId", order.getId(),
                                "reason", "Pedido cancelado automáticamente por falta de confirmación tras 5 minutos.",
                                "status", OrderStatus.CANCELLED.name()
                        )
                );

                LOG.info(() -> String.format(
                        "[TIMEOUT-RESTAURANTE] Cancelación y notificaciones despachadas con éxito para orderId=%s",
                        orderId));
            } else {
                LOG.fine(() -> String.format(
                        "[REDIS-KEYSPACE] Pedido %s ya fue procesado (estado actual: %s). No se requiere acción.",
                        orderId, order.getStatus()));
            }

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex, () -> String.format(
                    "[REDIS-KEYSPACE][ERROR] Excepción procesando timeout de aceptación para pedido %s",
                    orderId));
        }
    }
}
