package com.delivery.domain.enums;

/**
 * Tipos de notificación emitidos por el sistema.
 *
 * <p>Cada tipo define el evento de negocio que originó la notificación
 * y determina la plantilla del mensaje, los canales de envío
 * (push, SMS, email) y los destinatarios.</p>
 *
 * @since 1.0.0
 */
public enum NotificationType {

    // ── Eventos de pedido (destinatario: MERCHANT) ──────────────────

    /** Nuevo pedido creado — notifica al comercio para aceptación */
    ORDER_CREATED,

    /** Pedido cancelado — notifica al comercio para detener preparación */
    ORDER_CANCELLED,

    // ── Eventos de estado (destinatario: CUSTOMER) ──────────────────

    /** Estado del pedido actualizado — notifica al cliente */
    STATUS_UPDATED,

    /** Repartidor asignado — notifica al cliente con datos del repartidor */
    DRIVER_ASSIGNED,

    /** Repartidor llegó al punto de entrega */
    DRIVER_ARRIVED,

    // ── Eventos de entrega (destinatario: CUSTOMER / DRIVER) ────────

    /** PIN de entrega generado — envía el PIN al cliente */
    DELIVERY_PIN_GENERATED,

    /** Entrega confirmada exitosamente */
    DELIVERY_CONFIRMED,

    // ── Eventos de incidente ────────────────────────────────────────

    /** Incidente reportado durante la entrega */
    INCIDENT_REPORTED,

    /** Incidente resuelto — ruta retomada */
    INCIDENT_RESOLVED
}
