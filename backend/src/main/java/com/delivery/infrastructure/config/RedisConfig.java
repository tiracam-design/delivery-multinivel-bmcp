package com.delivery.infrastructure.config;

import com.delivery.infrastructure.adapter.output.redis.listener.OrderUnacceptedExpirationListener;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Configuración central de Redis para el Sistema de Delivery Multinivel.
 *
 * <p>Provee:
 * <ul>
 *   <li>{@link RedisTemplate} con serialización JSON para objetos y coordenadas GPS.</li>
 *   <li>{@link StringRedisTemplate} optimizado para contadores atómicos y Rate Limiting.</li>
 *   <li>{@link RedisMessageListenerContainer} configurado para escuchar eventos de expiración
 *       de claves (Keyspace Notifications: {@code __keyevent@*__:expired}).</li>
 * </ul>
 * </p>
 *
 * @since 1.0.0
 */
@Configuration
@ConditionalOnBean(RedisConnectionFactory.class)
public class RedisConfig {

    private static final String EXPIRED_EVENTS_TOPIC = "__keyevent@*__:expired";

    /**
     * ObjectMapper personalizado con soporte para Java 8/21 Date/Time (Instant, LocalDateTime).
     */
    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    /**
     * RedisTemplate genérico con serialización de llaves en String y valores en JSON.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper redisObjectMapper) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(redisObjectMapper);

        // Claves como String plano
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Valores serializados en JSON con soporte de fechas ISO-8601
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * StringRedisTemplate para operaciones rápidas basadas en texto (Rate Limiting de PIN y locks).
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    /**
     * Contenedor de escucha de eventos pub/sub de Redis para Keyspace Notifications.
     * Escucha el canal: {@code __keyevent@*__:expired}
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            OrderUnacceptedExpirationListener orderExpirationListener) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(orderExpirationListener, new PatternTopic(EXPIRED_EVENTS_TOPIC));
        return container;
    }
}
