package com.delivery.domain.port.output;

import com.delivery.domain.model.GeoLocation;

import java.util.UUID;

/**
 * Puerto de Salida — Caché Volátil de Geolocalización en Tiempo Real (Redis).
 *
 * <p>Operaciones de telemetría de ultra-baja latencia con TTL corto (15 segundos)
 * para el tracking en vivo del repartidor durante un pedido activo.</p>
 *
 * @since 1.0.0
 */
public interface DriverLiveLocationPort {

    /**
     * Guarda o refresca las coordenadas GPS del repartidor en memoria volátil (Redis)
     * con TTL estricto de 15 segundos.
     *
     * @param driverId identificador del repartidor
     * @param location coordenada geográfica
     * @param heading  orientación en grados (0-360)
     * @param speed    velocidad en m/s
     */
    void updateDriverLocation(UUID driverId, GeoLocation location, Double heading, Double speed);

    /**
     * Obtiene las coordenadas más recientes desde la caché volátil.
     * Retorna {@code null} si la clave expiró por falta de actualización en 15s.
     *
     * @param driverId identificador del repartidor
     * @return coordenada geográfica o null si no hay señal reciente
     */
    GeoLocation findLatestLocation(UUID driverId);

    /**
     * Elimina las coordenadas en memoria (ej. al desconectarse el repartidor).
     *
     * @param driverId identificador del repartidor
     */
    void evictDriverLocation(UUID driverId);
}
