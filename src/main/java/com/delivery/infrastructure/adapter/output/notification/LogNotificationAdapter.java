package com.delivery.infrastructure.adapter.output.notification;

import com.delivery.domain.enums.NotificationType;
import com.delivery.domain.port.output.NotificationPort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Adaptador de Salida — Notificaciones por log.
 *
 * <p>Implementación inicial que registra las notificaciones en el log
 * de la aplicación. En producción se reemplazará por un adaptador
 * que envíe notificaciones reales (Firebase, SMS, email, WebSocket, etc.).</p>
 *
 * @since 1.0.0
 */
@Component
public class LogNotificationAdapter implements NotificationPort {

    private static final Logger log = LoggerFactory.getLogger(LogNotificationAdapter.class);

    @Override
    public void notifyMerchant(UUID merchantId, NotificationType type,
                               Map<String, Object> payload) {
        log.info("📢 [MERCHANT:{}] {} — {}", merchantId, type, payload);
    }

    @Override
    public void notifyCustomer(UUID customerId, NotificationType type,
                               Map<String, Object> payload) {
        log.info("📢 [CUSTOMER:{}] {} — {}", customerId, type, payload);
    }

    @Override
    public void notifyDriver(UUID driverId, NotificationType type,
                             Map<String, Object> payload) {
        log.info("📢 [DRIVER:{}] {} — {}", driverId, type, payload);
    }
}
