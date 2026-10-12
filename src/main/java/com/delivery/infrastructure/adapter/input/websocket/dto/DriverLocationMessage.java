package com.delivery.infrastructure.adapter.input.websocket.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * DTO de entrada para los mensajes de ubicación GPS del repartidor.
 *
 * <p>Recibido cada 3-5 segundos a través del canal STOMP
 * {@code /app/driver/location}. Contiene las coordenadas GPS
 * en tiempo real junto con los identificadores de contexto.</p>
 *
 * <h3>Validaciones:</h3>
 * <ul>
 *   <li>Latitud:  rango WGS 84 [{@code -90}, {@code +90}]</li>
 *   <li>Longitud: rango WGS 84 [{@code -180}, {@code +180}]</li>
 *   <li>Todos los campos son obligatorios</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class DriverLocationMessage {

    @NotNull(message = "driverId es obligatorio")
    private UUID driverId;

    @NotNull(message = "orderId es obligatorio")
    private UUID orderId;

    @NotNull(message = "latitude es obligatorio")
    @DecimalMin(value = "-90.0", message = "latitude fuera de rango [-90, 90]")
    @DecimalMax(value = "90.0", message = "latitude fuera de rango [-90, 90]")
    private Double latitude;

    @NotNull(message = "longitude es obligatorio")
    @DecimalMin(value = "-180.0", message = "longitude fuera de rango [-180, 180]")
    @DecimalMax(value = "180.0", message = "longitude fuera de rango [-180, 180]")
    private Double longitude;

    /** Constructor vacío requerido por Jackson para deserialización STOMP. */
    public DriverLocationMessage() {
    }

    public DriverLocationMessage(UUID driverId, UUID orderId,
                                 Double latitude, Double longitude) {
        this.driverId = driverId;
        this.orderId = orderId;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // ── Getters & Setters ───────────────────────────────────────────

    public UUID getDriverId() { return driverId; }
    public void setDriverId(UUID driverId) { this.driverId = driverId; }

    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    @Override
    public String toString() {
        return String.format(
                "DriverLocationMessage[driverId=%s, orderId=%s, lat=%.6f, lon=%.6f]",
                driverId, orderId, latitude, longitude);
    }
}
