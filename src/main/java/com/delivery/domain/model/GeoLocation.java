package com.delivery.domain.model;

import java.util.Objects;

/**
 * Value Object inmutable que representa una coordenada geográfica
 * (latitud, longitud) en el sistema de referencia WGS 84 (SRID 4326).
 *
 * <p>Se usa para las ubicaciones de pickup y delivery de los pedidos,
 * posiciones de repartidores y límites de zonas de entrega.</p>
 *
 * <h3>Rangos válidos:</h3>
 * <ul>
 *   <li>Latitud:  {@code -90.0} a {@code +90.0}</li>
 *   <li>Longitud: {@code -180.0} a {@code +180.0}</li>
 * </ul>
 *
 * @since 1.0.0
 */
public final class GeoLocation {

    private final double latitude;
    private final double longitude;

    /**
     * @param latitude  latitud WGS 84 ({@code -90} a {@code +90})
     * @param longitude longitud WGS 84 ({@code -180} a {@code +180})
     * @throws IllegalArgumentException si las coordenadas están fuera de rango
     */
    public GeoLocation(double latitude, double longitude) {
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException(
                    "Latitud fuera de rango [-90, 90]: " + latitude);
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException(
                    "Longitud fuera de rango [-180, 180]: " + longitude);
        }
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /** @return latitud en grados decimales */
    public double getLatitude() {
        return latitude;
    }

    /** @return longitud en grados decimales */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Calcula la distancia aproximada en kilómetros usando la
     * fórmula de Haversine.
     *
     * @param other punto de destino
     * @return distancia en kilómetros
     */
    public double distanceKmTo(GeoLocation other) {
        final double R = 6_371.0; // radio de la Tierra en km
        double dLat = Math.toRadians(other.latitude - this.latitude);
        double dLon = Math.toRadians(other.longitude - this.longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(this.latitude))
                * Math.cos(Math.toRadians(other.latitude))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GeoLocation that)) return false;
        return Double.compare(latitude, that.latitude) == 0
                && Double.compare(longitude, that.longitude) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(latitude, longitude);
    }

    @Override
    public String toString() {
        return String.format("GeoLocation[lat=%.6f, lon=%.6f]", latitude, longitude);
    }
}
