package com.delivery.infrastructure.adapter.output.redis;

import com.delivery.domain.model.GeoLocation;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisDriverLocationAdapter — GPS Tracking con TTL 15s")
class RedisDriverLocationAdapterTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ObjectMapper objectMapper;
    private RedisDriverLocationAdapter adapter;
    private UUID driverId;
    private String expectedKey;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        adapter = new RedisDriverLocationAdapter(stringRedisTemplate, objectMapper);
        driverId = UUID.randomUUID();
        expectedKey = "driver:location:" + driverId;
    }

    @Test
    @DisplayName("Guarda coordenadas con clave driver:location:{id}, JSON y TTL estricto de 15 segundos")
    void updateDriverLocation_savesJsonWith15SecondsTtl() {
        GeoLocation location = new GeoLocation(-0.8123, -77.7123);

        adapter.updateDriverLocation(driverId, location, 180.0, 35.5);

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(
                eq(expectedKey),
                jsonCaptor.capture(),
                eq(Duration.ofSeconds(15))
        );

        String capturedJson = jsonCaptor.getValue();
        assertTrue(capturedJson.contains("\"latitude\":-0.8123"));
        assertTrue(capturedJson.contains("\"longitude\":-77.7123"));
        assertTrue(capturedJson.contains("\"updatedAt\""));
    }

    @Test
    @DisplayName("Recupera coordenadas vigentes deserializando el JSON")
    void findLatestLocation_whenKeyPresent_returnsGeoLocation() {
        String json = "{\"latitude\":-0.8123,\"longitude\":-77.7123,\"updatedAt\":\"2026-10-12T12:00:00Z\"}";
        when(valueOperations.get(expectedKey)).thenReturn(json);

        GeoLocation location = adapter.findLatestLocation(driverId);

        assertNotNull(location);
        assertEquals(-0.8123, location.getLatitude(), 0.00001);
        assertEquals(-77.7123, location.getLongitude(), 0.00001);
    }

    @Test
    @DisplayName("Si la clave expiró tras 15s sin señal, retorna null")
    void findLatestLocation_whenExpired_returnsNull() {
        when(valueOperations.get(expectedKey)).thenReturn(null);

        GeoLocation location = adapter.findLatestLocation(driverId);

        assertNull(location);
    }
}
