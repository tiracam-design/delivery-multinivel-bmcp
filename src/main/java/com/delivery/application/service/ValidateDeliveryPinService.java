package com.delivery.application.service;

import com.delivery.domain.enums.NotificationType;
import com.delivery.domain.enums.OrderStatus;
import com.delivery.domain.exception.InvalidDeliveryPinException;
import com.delivery.domain.exception.InvalidStateTransitionException;
import com.delivery.domain.exception.OrderNotFoundException;
import com.delivery.domain.model.Order;
import com.delivery.domain.port.input.ValidateDeliveryPinUseCase;
import com.delivery.domain.port.input.command.ValidateDeliveryPinCommand;
import com.delivery.domain.port.output.NotificationPort;
import com.delivery.domain.port.output.OrderRepositoryPort;
import com.delivery.domain.port.output.PinRateLimiterPort;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Implementación del caso de uso: Validar PIN de Entrega (Anti-Fraude).
 *
 * <p>Orquesta el flujo completo de verificación del PIN OTP según
 * RULE-002 y REQ-SEC-001 de la BMCP v1.0.0, incorporando control
 * distribuido de intentos mediante Redis Rate Limiting.</p>
 *
 * @see ValidateDeliveryPinUseCase
 * @see Order#confirmDelivery(String)
 * @since 1.0.0
 */
public class ValidateDeliveryPinService implements ValidateDeliveryPinUseCase {

    private static final Logger LOG = Logger.getLogger(
            ValidateDeliveryPinService.class.getName());

    private final OrderRepositoryPort orderRepository;
    private final NotificationPort notificationPort;
    private final PinRateLimiterPort pinRateLimiterPort;

    public ValidateDeliveryPinService(OrderRepositoryPort orderRepository,
                                      NotificationPort notificationPort) {
        this(orderRepository, notificationPort, null);
    }

    public ValidateDeliveryPinService(OrderRepositoryPort orderRepository,
                                      NotificationPort notificationPort,
                                      PinRateLimiterPort pinRateLimiterPort) {
        this.orderRepository = Objects.requireNonNull(orderRepository,
                "orderRepository no puede ser null");
        this.notificationPort = Objects.requireNonNull(notificationPort,
                "notificationPort no puede ser null");
        this.pinRateLimiterPort = pinRateLimiterPort;
    }

    @Override
    public boolean execute(ValidateDeliveryPinCommand command) {
        Objects.requireNonNull(command, "command no puede ser null");

        UUID orderId = command.getOrderId();
        String candidatePin = command.getPin();

        // ── Paso 0: Control de Rate Limiting en Redis (Bloqueo preventivo) ──
        if (pinRateLimiterPort != null) {
            pinRateLimiterPort.checkPinAttemptAllowed(orderId);
        }

        // ── Paso 1: Recuperar la orden ──────────────────────────────
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    LOG.warning(() -> String.format(
                            "[AUDIT] Intento de validar PIN para pedido inexistente. "
                                    + "orderId=%s", orderId));
                    return new OrderNotFoundException(orderId);
                });

        // ── Paso 2: Verificar estado ON_THE_WAY ─────────────────────
        if (order.getStatus() != OrderStatus.ON_THE_WAY) {
            LOG.warning(() -> String.format(
                    "[AUDIT] Intento de validar PIN en estado inválido. "
                            + "orderId=%s, status=%s (esperado: ON_THE_WAY)",
                    orderId, order.getStatus()));
            throw new InvalidStateTransitionException(
                    order.getStatus(), OrderStatus.DELIVERED);
        }

        // ── Paso 3: Validar PIN via modelo de dominio ───────────────
        try {
            order.confirmDelivery(candidatePin);
        } catch (InvalidDeliveryPinException ex) {
            // Persistir intento fallido en persistencia relacional
            orderRepository.save(order);

            // Registrar intento fallido en Redis Rate Limiting (5 min TTL)
            if (pinRateLimiterPort != null) {
                pinRateLimiterPort.recordFailedAttempt(orderId);
            }

            LOG.warning(() -> String.format(
                    "[AUDIT][RATE-LIMIT] PIN incorrecto para pedido. "
                            + "orderId=%s, intentosRestantes=%d, bloqueado=%s, "
                            + "motivo='%s'",
                    orderId,
                    order.getDeliveryPin().getRemainingAttempts(),
                    order.getDeliveryPin().isBlocked(),
                    ex.getMessage()));

            throw ex;
        }

        // ── Paso 4 (éxito): Limpiar rate limit, persistir y notificar ─
        if (pinRateLimiterPort != null) {
            pinRateLimiterPort.resetAttempts(orderId);
        }

        orderRepository.save(order);

        LOG.info(() -> String.format(
                "[AUDIT] Entrega confirmada exitosamente con PIN. "
                        + "orderId=%s, customerId=%s, driverId=%s",
                orderId, order.getCustomerId(), order.getDriverId()));

        // Notificar al cliente → inicia liquidación de fondos
        notificationPort.notifyCustomer(
                order.getCustomerId(),
                NotificationType.DELIVERY_CONFIRMED,
                buildDeliveryConfirmedPayload(order)
        );

        // Notificar al repartidor
        if (order.getDriverId() != null) {
            notificationPort.notifyDriver(
                    order.getDriverId(),
                    NotificationType.DELIVERY_CONFIRMED,
                    buildDeliveryConfirmedPayload(order)
            );
        }

        return true;
    }

    private Map<String, Object> buildDeliveryConfirmedPayload(Order order) {
        return Map.of(
                "orderId", order.getId(),
                "status", OrderStatus.DELIVERED.name(),
                "totalAmount", order.getTotalAmount(),
                "deliveryAddress", order.getDeliveryAddress(),
                "deliveredAt", order.getUpdatedAt().toString()
        );
    }
}
