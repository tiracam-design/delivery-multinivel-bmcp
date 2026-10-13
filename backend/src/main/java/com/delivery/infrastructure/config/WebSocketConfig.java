package com.delivery.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuración del Message Broker WebSocket con STOMP.
 *
 * <p>Habilita la comunicación bidireccional en tiempo real entre
 * repartidores y clientes para el tracking de entregas en vivo.</p>
 *
 * <h3>Topología de canales:</h3>
 * <ul>
 *   <li>{@code /topic/tracking/{orderId}} — canal público de tracking GPS por orden</li>
 *   <li>{@code /queue/*}                  — colas privadas por sesión de usuario</li>
 *   <li>{@code /app/*}                    — destinos de aplicación (controladores)</li>
 * </ul>
 *
 * <h3>Endpoint de conexión:</h3>
 * <pre>
 *   ws://host:port/ws-delivery          (WebSocket nativo)
 *   http://host:port/ws-delivery        (SockJS fallback)
 * </pre>
 *
 * @since 1.0.0
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configura el message broker con dos destinos:
     * <ul>
     *   <li>{@code /topic} — broadcast a todos los suscriptores (tracking GPS)</li>
     *   <li>{@code /queue} — mensajes punto-a-punto por sesión</li>
     * </ul>
     *
     * <p>El prefijo {@code /app} indica que los mensajes enviados por los
     * clientes serán ruteados a métodos {@code @MessageMapping} en los
     * controladores.</p>
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Broker simple en memoria para /topic (broadcast) y /queue (p2p)
        registry.enableSimpleBroker("/topic", "/queue");

        // Prefijo para destinos de aplicación → @MessageMapping
        registry.setApplicationDestinationPrefixes("/app");

        // Prefijo para mensajes dirigidos a un usuario específico
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * Registra el endpoint STOMP {@code /ws-delivery} con soporte
     * SockJS para navegadores que no soportan WebSocket nativo.
     *
     * <p>Se permite cualquier origen para desarrollo. En producción,
     * restringir con {@code setAllowedOrigins("https://app.delivery.com")}.</p>
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-delivery")
                .setAllowedOriginPatterns("*")  // TODO: restringir en producción
                .withSockJS();                  // fallback HTTP para navegadores legacy
    }
}
