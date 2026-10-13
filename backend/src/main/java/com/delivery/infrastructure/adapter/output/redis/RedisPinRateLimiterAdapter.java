package com.delivery.infrastructure.adapter.output.redis;

import com.delivery.domain.exception.TooManyPinAttemptsException;
import com.delivery.domain.port.output.PinRateLimiterPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Adaptador de Salida — Rate Limiting Distribuido con Redis para PIN OTP (Anti-Fraude).
 *
 * <p>Mecanismo:
 * <ul>
 *   <li>Clave: {@code order:pin_attempts:{orderId}}</li>
 *   <li>Contador atómico con {@code INCR}.</li>
 *   <li>TTL de ventana deslizante/bloqueo: 5 minutos ({@code Duration.ofMinutes(5)}).</li>
 *   <li>Umbral máximo: 3 intentos fallidos. Al alcanzarse, lanza {@link TooManyPinAttemptsException}.</li>
 * </ul>
 * </p>
 *
 * @see PinRateLimiterPort
 * @see TooManyPinAttemptsException
 * @since 1.0.0
 */
@Component
public class RedisPinRateLimiterAdapter implements PinRateLimiterPort {

    private static final Logger LOG = Logger.getLogger(RedisPinRateLimiterAdapter.class.getName());

    public static final String KEY_PREFIX = "order:pin_attempts:";
    public static final int MAX_ATTEMPTS = 3;
    public static final Duration LOCK_WINDOW = Duration.ofMinutes(5);

    private final StringRedisTemplate stringRedisTemplate;

    public RedisPinRateLimiterAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = Objects.requireNonNull(stringRedisTemplate,
                "stringRedisTemplate no puede ser null");
    }

    /**
     * Verifica preventivamente si el pedido ya está bloqueado por exceso de intentos.
     */
    @Override
    public void checkPinAttemptAllowed(UUID orderId) {
        Objects.requireNonNull(orderId, "orderId no puede ser null");
        String key = buildKey(orderId);
        String val = stringRedisTemplate.opsForValue().get(key);

        if (val != null) {
            try {
                int attempts = Integer.parseInt(val);
                if (attempts >= MAX_ATTEMPTS) {
                    LOG.warning(() -> String.format(
                            "[REDIS-RATE-LIMIT] Intento bloqueado. orderId=%s ya alcanzó %d intentos",
                            orderId, attempts));
                    throw new TooManyPinAttemptsException(orderId, attempts);
                }
            } catch (NumberFormatException ignored) {
                // Si el valor no es numérico, dejamos proceder
            }
        }
    }

    /**
     * Incrementa atómicamente el contador de fallos.
     * Si es el primer intento, fija el TTL a 5 minutos.
     * Si alcanza 3 intentos, lanza {@link TooManyPinAttemptsException}.
     */
    @Override
    public void recordFailedAttempt(UUID orderId) {
        Objects.requireNonNull(orderId, "orderId no puede ser null");
        String key = buildKey(orderId);

        Long attempts = stringRedisTemplate.opsForValue().increment(key);

        if (attempts != null && attempts == 1L) {
            // Primer intento fallido: iniciamos la ventana de 5 minutos
            stringRedisTemplate.expire(key, LOCK_WINDOW);
        }

        int currentAttempts = attempts != null ? attempts.intValue() : 1;

        LOG.warning(() -> String.format(
                "[REDIS-RATE-LIMIT] Intento fallido registrado para orderId=%s (%d/%d intentos, TTL=%d min)",
                orderId, currentAttempts, MAX_ATTEMPTS, LOCK_WINDOW.toMinutes()));

        if (currentAttempts >= MAX_ATTEMPTS) {
            LOG.severe(() -> String.format(
                    "[REDIS-RATE-LIMIT][BLOQUEADO] Pedido %s bloqueado por 5 minutos tras %d intentos fallidos",
                    orderId, currentAttempts));
            throw new TooManyPinAttemptsException(orderId, currentAttempts);
        }
    }

    /**
     * Resetea el contador tras una validación exitosa del PIN.
     */
    @Override
    public void resetAttempts(UUID orderId) {
        if (orderId != null) {
            stringRedisTemplate.delete(buildKey(orderId));
            LOG.fine(() -> String.format(
                    "[REDIS-RATE-LIMIT] Contador de intentos reseteado para orderId=%s tras éxito",
                    orderId));
        }
    }

    /**
     * Retorna la cantidad de intentos restantes.
     */
    @Override
    public int getRemainingAttempts(UUID orderId) {
        if (orderId == null) return MAX_ATTEMPTS;
        String val = stringRedisTemplate.opsForValue().get(buildKey(orderId));
        if (val == null) return MAX_ATTEMPTS;
        try {
            int used = Integer.parseInt(val);
            return Math.max(0, MAX_ATTEMPTS - used);
        } catch (NumberFormatException ex) {
            return MAX_ATTEMPTS;
        }
    }

    private String buildKey(UUID orderId) {
        return KEY_PREFIX + orderId.toString();
    }
}
