package com.delivery;

import com.delivery.domain.port.output.PinRateLimiterPort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class DeliveryMultinivelApplicationTests {

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @MockBean
    private PinRateLimiterPort pinRateLimiterPort;

    @Test
    void contextLoads() {
        // Verifica que el contexto de Spring se levante correctamente
    }
}
