package com.delivery.infrastructure.adapter.output.redis;

import com.delivery.domain.exception.TooManyPinAttemptsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisPinRateLimiterAdapter — Rate Limiting de PIN con TTL 5 min")
class RedisPinRateLimiterAdapterTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisPinRateLimiterAdapter adapter;
    private UUID orderId;
    private String expectedKey;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        adapter = new RedisPinRateLimiterAdapter(stringRedisTemplate);
        orderId = UUID.randomUUID();
        expectedKey = "order:pin_attempts:" + orderId;
    }

    @Test
    @DisplayName("Primer intento fallido: incrementa contador a 1 y fija TTL de 5 minutos")
    void firstFailedAttempt_setsTtl5Minutes() {
        when(valueOperations.increment(expectedKey)).thenReturn(1L);

        assertDoesNotThrow(() -> adapter.recordFailedAttempt(orderId));

        verify(valueOperations).increment(expectedKey);
        verify(stringRedisTemplate).expire(expectedKey, Duration.ofMinutes(5));
    }

    @Test
    @DisplayName("Segundo intento fallido: incrementa contador sin sobreescribir TTL inicial")
    void secondFailedAttempt_doesNotResetTtl() {
        when(valueOperations.increment(expectedKey)).thenReturn(2L);

        assertDoesNotThrow(() -> adapter.recordFailedAttempt(orderId));

        verify(valueOperations).increment(expectedKey);
        verify(stringRedisTemplate, never()).expire(eq(expectedKey), any(Duration.class));
    }

    @Test
    @DisplayName("Tercer intento fallido: alcanza umbral y lanza TooManyPinAttemptsException")
    void thirdFailedAttempt_throwsTooManyPinAttemptsException() {
        when(valueOperations.increment(expectedKey)).thenReturn(3L);

        TooManyPinAttemptsException ex = assertThrows(
                TooManyPinAttemptsException.class,
                () -> adapter.recordFailedAttempt(orderId)
        );

        assertEquals(orderId, ex.getOrderId());
        assertEquals(3, ex.getAttempts());
        assertTrue(ex.getMessage().contains("Límite de intentos excedido (3/3)"));
    }

    @Test
    @DisplayName("checkPinAttemptAllowed: lanza excepción preventivamente si ya hay 3 intentos")
    void checkPinAttemptAllowed_whenBlocked_throwsException() {
        when(valueOperations.get(expectedKey)).thenReturn("3");

        assertThrows(TooManyPinAttemptsException.class,
                () -> adapter.checkPinAttemptAllowed(orderId));
    }

    @Test
    @DisplayName("checkPinAttemptAllowed: permite intento si contador es menor a 3")
    void checkPinAttemptAllowed_whenBelowLimit_doesNotThrow() {
        when(valueOperations.get(expectedKey)).thenReturn("2");

        assertDoesNotThrow(() -> adapter.checkPinAttemptAllowed(orderId));
    }

    @Test
    @DisplayName("resetAttempts: elimina la clave de Redis tras éxito")
    void resetAttempts_deletesKey() {
        adapter.resetAttempts(orderId);

        verify(stringRedisTemplate).delete(expectedKey);
    }
}
