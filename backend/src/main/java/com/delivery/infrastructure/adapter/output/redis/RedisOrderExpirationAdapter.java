package com.delivery.infrastructure.adapter.output.redis;

import com.delivery.domain.port.output.OrderExpirationPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Adaptador de Salida — Programación de Expiración de Pedidos no Aceptados en Redis.
 *
 * <p>Al crearse un pedido, se almacena una clave {@code order:unaccepted:{orderId}}
 * con TTL estricto de 5 minutos. Si el restaurante acepta el pedido antes, se elimina la clave.
 * Si pasan 5 minutos sin aceptación, Redis Keyspace Notifications emite el evento de expiración.</p>
 *
 * @see OrderExpirationPort
 * @since 1.0.0
 */
@Component
public class RedisOrderExpirationAdapter implements OrderExpirationPort {

    private static final Logger LOG = Logger.getLogger(RedisOrderExpirationAdapter.class.getName());

    public static final String KEY_PREFIX = "order:unaccepted:";
    public static final Duration UNACCEPTED_TIMEOUT = Duration.ofMinutes(5);

    private final StringRedisTemplate stringRedisTemplate;

    public RedisOrderExpirationAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = Objects.requireNonNull(stringRedisTemplate,
                "stringRedisTemplate no puede ser null");
    }

    @Override
    public void scheduleUnacceptedOrderTimeout(UUID orderId) {
        Objects.requireNonNull(orderId, "orderId no puede ser null");
        String key = buildKey(orderId);
        stringRedisTemplate.opsForValue().set(key, "PENDING_MERCHANT_ACCEPTANCE", UNACCEPTED_TIMEOUT);

        LOG.info(() -> String.format(
                "[REDIS-TIMEOUT] Temporizador de aceptación fijado: key=%s, ttl=%d min",
                key, UNACCEPTED_TIMEOUT.toMinutes()));
    }

    @Override
    public void cancelUnacceptedOrderTimeout(UUID orderId) {
        if (orderId != null) {
            String key = buildKey(orderId);
            Boolean deleted = stringRedisTemplate.delete(key);
            LOG.info(() -> String.format(
                    "[REDIS-TIMEOUT] Temporizador de aceptación cancelado para pedido %s (eliminado=%s)",
                    orderId, deleted));
        }
    }

    public static String buildKey(UUID orderId) {
        return KEY_PREFIX + orderId.toString();
    }
}
