package com.delivery.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Interceptor de canal WebSocket para autenticación y autorización
 * a nivel de protocolo STOMP.
 *
 * <p>Intercepta las tramas STOMP en el canal de entrada (inbound) para:</p>
 * <ul>
 *   <li><strong>CONNECT</strong> — validar token JWT en el handshake</li>
 *   <li><strong>SUBSCRIBE</strong> — controlar acceso a topics restringidos</li>
 *   <li><strong>SEND</strong> — registrar mensajes entrantes para auditoría</li>
 * </ul>
 *
 * <p>Trabaja en conjunto con {@link WebSocketConfig} y la cadena de
 * seguridad de Spring Security.</p>
 *
 * @since 1.0.0
 */
@Configuration
public class WebSocketChannelInterceptorConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketChannelInterceptorConfig.class);

    /**
     * Registra el interceptor en el canal de entrada del cliente
     * para inspeccionar las tramas STOMP antes de que lleguen
     * a los controladores.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new StompSecurityInterceptor());
    }

    // ═══════════════════════════════════════════════════════════════
    //  Interceptor STOMP interno
    // ═══════════════════════════════════════════════════════════════

    /**
     * Interceptor que inspecciona tramas STOMP en el canal de entrada.
     *
     * <p>El flujo de autenticación JWT funciona así:</p>
     * <ol>
     *   <li>El cliente envía un CONNECT con el header
     *       {@code Authorization: Bearer &lt;token&gt;}</li>
     *   <li>Este interceptor extrae y valida el token</li>
     *   <li>Si es válido, establece el {@code Principal} en la sesión</li>
     *   <li>Mensajes subsiguientes heredan la identidad autenticada</li>
     * </ol>
     *
     * <p><strong>Nota:</strong> La validación real del JWT se delega
     * al JwtTokenProvider de la capa de seguridad cuando se implemente.
     * Actualmente se registra el intento de conexión para auditoría.</p>
     */
    private static class StompSecurityInterceptor implements ChannelInterceptor {

        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            StompHeaderAccessor accessor =
                    MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

            if (accessor == null || accessor.getCommand() == null) {
                return message;
            }

            StompCommand command = accessor.getCommand();

            switch (command) {
                case CONNECT -> {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        log.info("🔐 WebSocket CONNECT — token recibido (longitud={})",
                                token.length());

                        // TODO: Integrar con JwtTokenProvider para validar el token
                        //       y establecer el SecurityContext:
                        //
                        //   UserDetails user = jwtTokenProvider.validateAndGetUser(token);
                        //   UsernamePasswordAuthenticationToken auth =
                        //       new UsernamePasswordAuthenticationToken(
                        //           user, null, user.getAuthorities());
                        //   accessor.setUser(auth);
                        //
                        // Por ahora se acepta la conexión para permitir el desarrollo
                        // del flujo de tracking completo.
                    } else {
                        log.warn("🔐 WebSocket CONNECT — sin token de autenticación");
                    }
                }

                case SUBSCRIBE -> {
                    String destination = accessor.getDestination();
                    log.debug("📥 WebSocket SUBSCRIBE → {}", destination);

                    // TODO: Validar que el usuario tiene permiso para
                    //       suscribirse al topic solicitado.
                    //       Ejemplo: solo el cliente de la orden puede
                    //       suscribirse a /topic/tracking/{orderId}
                }

                case SEND -> {
                    String destination = accessor.getDestination();
                    log.debug("📤 WebSocket SEND → {}", destination);
                }

                default -> { /* otros comandos STOMP no requieren acción */ }
            }

            return message;
        }
    }
}
