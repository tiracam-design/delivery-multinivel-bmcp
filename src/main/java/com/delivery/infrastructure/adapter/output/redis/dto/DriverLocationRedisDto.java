package com.delivery.infrastructure.adapter.output.redis.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.Instant;

/**
 * DTO para serialización/deserialización de coordenadas GPS de repartidores en Redis.
 *
 * <p>Estructura JSON:
 * {@code {"latitude": -0.8123, "longitude": -77.7123, "updatedAt": "2026-10-12T17:15:30Z"}}</p>
 *
 * @since 1.0.0
 */
public class DriverLocationRedisDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("latitude")
    private double latitude;

    @JsonProperty("longitude")
    private double longitude;

    @JsonProperty("updatedAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant updatedAt;

    @JsonProperty("heading")
    private Double heading;

    @JsonProperty("speed")
    private Double speed;

    public DriverLocationRedisDto() {
    }

    public DriverLocationRedisDto(double latitude, double longitude, Instant updatedAt) {
        this(latitude, longitude, updatedAt, null, null);
    }

    public DriverLocationRedisDto(double latitude, double longitude, Instant updatedAt,
                                  Double heading, Double speed) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.updatedAt = updatedAt;
        this.heading = heading;
        this.speed = speed;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Double getHeading() {
        return heading;
    }

    public void setHeading(Double heading) {
        this.heading = heading;
    }

    public Double getSpeed() {
        return speed;
    }

    public void setSpeed(Double speed) {
        this.speed = speed;
    }
}
