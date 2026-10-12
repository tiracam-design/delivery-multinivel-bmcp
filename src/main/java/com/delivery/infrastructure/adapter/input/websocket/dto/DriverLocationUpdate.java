package com.delivery.infrastructure.adapter.input.websocket.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO de salida con la ubicación del repartidor transmitida al canal
 * {@code /topic/tracking/{orderId}}.
 *
 * <p>Enriquece los datos GPS originales con un timestamp del servidor
 * para que el cliente pueda calcular latencia y renderizar la posición
 * en el mapa con precisión temporal.</p>
 *
 * @since 1.0.0
 */
public class DriverLocationUpdate {

    private UUID driverId;
    private UUID orderId;
    private double latitude;
    private double longitude;
    private Instant timestamp;

    /** Constructor vacío para serialización Jackson. */
    public DriverLocationUpdate() {
    }

    public DriverLocationUpdate(UUID driverId, UUID orderId,
                                double latitude, double longitude,
                                Instant timestamp) {
        this.driverId = driverId;
        this.orderId = orderId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
    }

    /**
     * Factory method que convierte un mensaje de entrada del repartidor
     * en un evento de tracking con timestamp del servidor.
     *
     * @param message mensaje GPS recibido del repartidor
     * @return evento de ubicación listo para broadcast
     */
    public static DriverLocationUpdate from(DriverLocationMessage message) {
        return new DriverLocationUpdate(
                message.getDriverId(),
                message.getOrderId(),
                message.getLatitude(),
                message.getLongitude(),
                Instant.now()
        );
    }

    // ── Getters & Setters ───────────────────────────────────────────

    public UUID getDriverId() { return driverId; }
    public void setDriverId(UUID driverId) { this.driverId = driverId; }

    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return String.format(
                "DriverLocationUpdate[driverId=%s, orderId=%s, lat=%.6f, lon=%.6f, ts=%s]",
                driverId, orderId, latitude, longitude, timestamp);
    }
}
