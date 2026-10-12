package com.delivery.domain.port.input.command;

import java.util.Objects;
import java.util.UUID;

/**
 * Comando inmutable para la asignación del repartidor más cercano.
 *
 * @see com.delivery.domain.port.input.AssignNearestDriverUseCase
 * @since 1.0.0
 */
public final class AssignNearestDriverCommand {

    public static final double DEFAULT_RADIUS_KM = 5.0;
    public static final double MAX_RADIUS_KM = 50.0;

    private final UUID orderId;
    private final double radiusKm;

    public AssignNearestDriverCommand(UUID orderId, double radiusKm) {
        this.orderId = Objects.requireNonNull(orderId, "orderId es obligatorio");
        if (radiusKm <= 0 || radiusKm > MAX_RADIUS_KM) {
            throw new IllegalArgumentException(String.format(
                    "radiusKm debe estar entre (0, %.0f], recibido: %.2f",
                    MAX_RADIUS_KM, radiusKm));
        }
        this.radiusKm = radiusKm;
    }

    public static AssignNearestDriverCommand withDefaultRadius(UUID orderId) {
        return new AssignNearestDriverCommand(orderId, DEFAULT_RADIUS_KM);
    }

    public UUID getOrderId() { return orderId; }
    public double getRadiusKm() { return radiusKm; }

    @Override
    public String toString() {
        return String.format("AssignNearestDriverCommand[orderId=%s, radiusKm=%.1f]",
                orderId, radiusKm);
    }
}
