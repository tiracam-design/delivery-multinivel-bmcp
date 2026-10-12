package com.delivery.infrastructure.adapter.output.redis;

import com.delivery.domain.model.GeoLocation;
import com.delivery.domain.port.output.DriverLiveLocationPort;
import com.delivery.infrastructure.adapter.output.redis.dto.DriverLocationRedisDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adaptador de Salida — Geolocalización en Tiempo Real de Repartidores en Redis (L1 Cache).
 *
 * <p>Responsabilidad:
 * <ul>
 *   <li>Almacenar coordenadas GPS instantáneas con clave {@code driver:location:{driverId}}.</li>
 *   <li>Formato de valor en JSON: {@code {"latitude": -0.8123, "longitude": -77.7123, "updatedAt": "..."}}.</li>
 *   <li>Aplicar un TTL estricto de 15 segundos para auto-expirar repartidores sin conexión o sin telemetría reciente.</li>
 * </ul>
 * </p>
 *
 * @see DriverLiveLocationPort
 * @since 1.0.0
 */
@Component
public class RedisDriverLocationAdapter implements DriverLiveLocationPort {

    private static final Logger LOG = Logger.getLogger(RedisDriverLocationAdapter.class.getName());

    public static final String KEY_PREFIX = "driver:location:";
    public static final Duration LOCATION_TTL = Duration.ofSeconds(15);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisDriverLocationAdapter(StringRedisTemplate stringRedisTemplate,
                                      ObjectMapper objectMapper) {
        this.stringRedisTemplate = Objects.requireNonNull(stringRedisTemplate,
                "stringRedisTemplate no puede ser null");
        this.objectMapper = Objects.requireNonNull(objectMapper,
                "objectMapper no puede ser null");
    }

    /**
     * Guarda o refresca las coordenadas GPS del repartidor en Redis con TTL de 15 segundos.
     *
     * @param driverId identificador único del repartidor
     * @param location coordenada geográfica
     * @param heading  orientación en grados (0-360)
     * @param speed    velocidad en m/s
     */
    @Override
    public void updateDriverLocation(UUID driverId, GeoLocation location, Double heading, Double speed) {
        Objects.requireNonNull(driverId, "driverId no puede ser null");
        Objects.requireNonNull(location, "location no puede ser null");

        String key = buildKey(driverId);
        DriverLocationRedisDto payload = new DriverLocationRedisDto(
                location.getLatitude(),
                location.getLongitude(),
                Instant.now(),
                heading,
                speed
        );

        try {
            String jsonValue = objectMapper.writeValueAsString(payload);
            stringRedisTemplate.opsForValue().set(key, jsonValue, LOCATION_TTL);

            LOG.fine(() -> String.format(
                    "[REDIS-GPS] Telemetría actualizada. key=%s, lat=%.6f, lon=%.6f, ttl=%ds",
                    key, location.getLatitude(), location.getLongitude(), LOCATION_TTL.toSeconds()));
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex, () -> String.format(
                    "[REDIS-GPS][ERROR] No se pudo guardar la ubicación del repartidor %s en Redis",
                    driverId));
        }
    }

    /**
     * Recupera la ubicación más reciente del repartidor desde la memoria volátil de Redis.
     * Si no ha emitido señal en los últimos 15 segundos, la clave habrá expirado y retornará null.
     *
     * @param driverId identificador del repartidor
     * @return GeoLocation si la clave está activa y dentro de los 15s, o null si expiró/no existe
     */
    @Override
    public GeoLocation findLatestLocation(UUID driverId) {
        Objects.requireNonNull(driverId, "driverId no puede ser null");
        String key = buildKey(driverId);
        String json = stringRedisTemplate.opsForValue().get(key);

        if (json == null || json.isBlank()) {
            LOG.fine(() -> String.format(
                    "[REDIS-GPS] Sin ubicación reciente para repartidor %s (clave expirada o inexistente)",
                    driverId));
            return null;
        }

        try {
            DriverLocationRedisDto dto = objectMapper.readValue(json, DriverLocationRedisDto.class);
            return new GeoLocation(dto.getLatitude(), dto.getLongitude());
        } catch (Exception ex) {
            LOG.log(Level.WARNING, ex, () -> String.format(
                    "[REDIS-GPS][ERROR] Error al deserializar ubicación para repartidor %s. json='%s'",
                    driverId, json));
            return null;
        }
    }

    /**
     * Retorna el DTO completo con timestamp y telemetría de forma opcional.
     */
    public Optional<DriverLocationRedisDto> findLatestLocationDetails(UUID driverId) {
        Objects.requireNonNull(driverId, "driverId no puede ser null");
        String key = buildKey(driverId);
        String json = stringRedisTemplate.opsForValue().get(key);

        if (json == null || json.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(json, DriverLocationRedisDto.class));
        } catch (Exception ex) {
            LOG.log(Level.WARNING, ex, () -> "Error al parsear DriverLocationRedisDto de Redis");
            return Optional.empty();
        }
    }

    /**
     * Elimina manualmente la ubicación del repartidor (ej. al desconectarse o finalizar turno).
     */
    public void evictDriverLocation(UUID driverId) {
        if (driverId != null) {
            stringRedisTemplate.delete(buildKey(driverId));
        }
    }

    private String buildKey(UUID driverId) {
        return KEY_PREFIX + driverId.toString();
    }
}
