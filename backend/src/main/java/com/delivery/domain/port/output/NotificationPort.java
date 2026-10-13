package com.delivery.domain.port.output;

import com.delivery.domain.enums.NotificationType;

import java.util.Map;
import java.util.UUID;

/**
 * Puerto de Salida — Notificaciones del Sistema.
 *
 * <p>Define <strong>qué</strong> notificar y a <strong>quién</strong>,
 * no <strong>cómo</strong>. El canal de envío es decisión de infraestructura.</p>
 *
 * @see NotificationType
 * @since 1.0.0
 */
public interface NotificationPort {

    void notifyMerchant(UUID merchantId, NotificationType type,
                        Map<String, Object> payload);

    void notifyCustomer(UUID customerId, NotificationType type,
                        Map<String, Object> payload);

    void notifyDriver(UUID driverId, NotificationType type,
                      Map<String, Object> payload);
}
