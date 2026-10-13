package com.delivery.domain.model;

import com.delivery.domain.enums.DeliveryLevel;

import java.util.Objects;
import java.util.UUID;

/**
 * Modelo de dominio que representa un repartidor del sistema multinivel.
 *
 * <p>POJO puro sin dependencias de framework. Proyección reducida
 * del agregado completo de Usuario, con los datos relevantes para
 * el módulo de Pedidos.</p>
 *
 * @since 1.0.0
 */
public class Driver {

    private final UUID id;
    private final String fullName;
    private final String phone;
    private final DeliveryLevel level;
    private final GeoLocation currentLocation;
    private final boolean available;
    private final Double distanceKm;

    public Driver(UUID id, String fullName, String phone,
                  DeliveryLevel level, GeoLocation currentLocation,
                  boolean available, Double distanceKm) {
        this.id = Objects.requireNonNull(id, "id no puede ser null");
        this.fullName = Objects.requireNonNull(fullName, "fullName no puede ser null");
        this.phone = phone;
        this.level = Objects.requireNonNull(level, "level no puede ser null");
        this.currentLocation = currentLocation;
        this.available = available;
        this.distanceKm = distanceKm;
    }

    public Driver(UUID id, String fullName, String phone,
                  DeliveryLevel level, GeoLocation currentLocation,
                  boolean available) {
        this(id, fullName, phone, level, currentLocation, available, null);
    }

    public UUID getId() { return id; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public DeliveryLevel getLevel() { return level; }
    public GeoLocation getCurrentLocation() { return currentLocation; }
    public boolean isAvailable() { return available; }
    public boolean isOnline() { return available; }
    public Double getDistanceKm() { return distanceKm; }

    /**
     * Mayor prioridad = nivel superior; a igual nivel, menor distancia.
     */
    public boolean hasHigherPriorityThan(Driver other) {
        if (this.level.ordinal() != other.level.ordinal()) {
            return this.level.ordinal() > other.level.ordinal();
        }
        if (this.distanceKm != null && other.distanceKm != null) {
            return this.distanceKm < other.distanceKm;
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Driver driver)) return false;
        return Objects.equals(id, driver.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return String.format("Driver[id=%s, name='%s', level=%s, available=%s, distance=%s km]",
                id, fullName, level, available,
                distanceKm != null ? String.format("%.1f", distanceKm) : "N/A");
    }
}
