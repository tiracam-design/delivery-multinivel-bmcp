package com.delivery.domain.model;

import com.delivery.domain.enums.OrderStatus;

import java.time.Instant;
import java.util.Objects;

/**
 * Value Object inmutable que registra una transición de estado
 * de un pedido, formando parte del historial de auditoría.
 *
 * <p>Cada vez que un pedido cambia de estado, se crea un nuevo
 * {@code OrderStatusChange} y se agrega al historial del pedido.
 * Esto permite:</p>
 * <ul>
 *   <li>Auditoría completa del ciclo de vida del pedido</li>
 *   <li>Cálculo de tiempos entre estados (SLA, métricas)</li>
 *   <li>Investigación de incidentes y disputas</li>
 *   <li>Análisis de rendimiento de repartidores</li>
 * </ul>
 *
 * @since 1.0.0
 */
public final class OrderStatusChange {

    private final OrderStatus fromStatus;
    private final OrderStatus toStatus;
    private final Instant changedAt;
    private final String reason;

    /**
     * @param fromStatus estado origen ({@code null} para el estado inicial)
     * @param toStatus   estado destino (no nulo)
     * @param changedAt  momento de la transición (no nulo)
     * @param reason     motivo de la transición (puede ser {@code null})
     */
    public OrderStatusChange(OrderStatus fromStatus,
                             OrderStatus toStatus,
                             Instant changedAt,
                             String reason) {
        this.fromStatus = fromStatus;
        this.toStatus = Objects.requireNonNull(toStatus, "toStatus no puede ser null");
        this.changedAt = Objects.requireNonNull(changedAt, "changedAt no puede ser null");
        this.reason = reason;
    }

    /**
     * @return estado origen, {@code null} si es la creación inicial
     */
    public OrderStatus getFromStatus() {
        return fromStatus;
    }

    /**
     * @return estado destino (nunca {@code null})
     */
    public OrderStatus getToStatus() {
        return toStatus;
    }

    /**
     * @return momento en que ocurrió la transición
     */
    public Instant getChangedAt() {
        return changedAt;
    }

    /**
     * @return motivo descriptivo (puede ser {@code null})
     */
    public String getReason() {
        return reason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderStatusChange that)) return false;
        return fromStatus == that.fromStatus
                && toStatus == that.toStatus
                && Objects.equals(changedAt, that.changedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromStatus, toStatus, changedAt);
    }

    @Override
    public String toString() {
        return String.format("StatusChange[%s → %s at %s | %s]",
                fromStatus, toStatus, changedAt,
                reason != null ? reason : "sin motivo");
    }
}
