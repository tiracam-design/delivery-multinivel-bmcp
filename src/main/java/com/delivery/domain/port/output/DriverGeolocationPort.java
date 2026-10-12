package com.delivery.domain.port.output;

import com.delivery.domain.model.Driver;
import com.delivery.domain.model.GeoLocation;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de Salida — Geolocalización de Repartidores.
 *
 * <p>Operaciones geoespaciales sobre ubicación de repartidores.
 * Implementación esperada: PostGIS ({@code ST_DWithin}, {@code ST_Distance})
 * sobre índices GIST.</p>
 *
 * @see Driver
 * @see GeoLocation
 * @since 1.0.0
 */
public interface DriverGeolocationPort {

    List<Driver> findAvailableDriversWithinRadius(GeoLocation center,
                                                  double radiusKm);

    /**
     * Busca repartidores con estado activo/online (is_online = true)
     * dentro de una distancia máxima en metros (ej. 3000 m) desde el restaurante.
     *
     * @param center       ubicación de referencia (restaurante)
     * @param radiusMeters radio de búsqueda en metros
     * @return lista de repartidores online dentro del radio especificado
     */
    default List<Driver> findOnlineDriversWithinRadiusMeters(GeoLocation center, double radiusMeters) {
        return findAvailableDriversWithinRadius(center, radiusMeters / 1000.0);
    }

    void updateDriverLocation(UUID driverId, GeoLocation location,
                              Double heading, Double speed);

    GeoLocation findLatestLocation(UUID driverId);
}
