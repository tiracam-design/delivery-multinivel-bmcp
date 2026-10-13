package com.delivery.infrastructure.adapter.input.websocket;

import com.delivery.domain.model.GeoLocation;
import com.delivery.domain.port.output.DriverGeolocationPort;
import com.delivery.infrastructure.adapter.input.websocket.dto.DriverLocationMessage;
import com.delivery.infrastructure.adapter.input.websocket.dto.DriverLocationUpdate;
import com.delivery.infrastructure.adapter.input.websocket.DriverLocationSecurityValidator.ValidationResult;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.core.MessageSendingOperations;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import java.util.Objects;

/**
 * Adaptador de Entrada WebSocket — DriverLocationWebSocketHandler.
 *
 * <p>Recibe coordenadas GPS de telemetría emitidas por la aplicación del repartidor
 * cada 3 segundos (latitude, longitude, orderId, driverId) y las retransmite
 * en tiempo real al canal STOMP {@code /topic/orders/{orderId}/tracking}.</p>
 *
 * <h3>Flujo de telemetría GPS:</h3>
 * <ol>
 *   <li>Repartidor envía coordenadas cada 3s a {@code /app/driver/location}.</li>
 *   <li>Se valida la integridad geográfica y la seguridad (repartidor asignado a orden activa).</li>
 *   <li>Se persiste la posición actual en PostGIS vía {@link DriverGeolocationPort}.</li>
 *   <li>Se difunde la posición al canal de tracking del cliente: {@code /topic/orders/{orderId}/tracking}.</li>
 * </ol>
 *
 * @since 1.0.0
 */
@Controller
@Validated
public class DriverLocationWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(DriverLocationWebSocketHandler.class);

    /** Canal STOMP principal según especificación del frontend y cliente */
    private static final String ORDERS_TRACKING_TOPIC_PREFIX = "/topic/orders/";
    private static final String ORDERS_TRACKING_TOPIC_SUFFIX = "/tracking";

    /** Canal alternativo para retrocompatibilidad */
    private static final String LEGACY_TRACKING_TOPIC_PREFIX = "/topic/tracking/";

    /** Cola privada de errores */
    private static final String ERROR_QUEUE = "/queue/errors";

    private final MessageSendingOperations<String> messagingTemplate;
    private final DriverLocationSecurityValidator securityValidator;
    private final DriverGeolocationPort driverGeolocationPort;

    public DriverLocationWebSocketHandler(MessageSendingOperations<String> messagingTemplate,
                                          DriverLocationSecurityValidator securityValidator,
                                          DriverGeolocationPort driverGeolocationPort) {
        this.messagingTemplate = Objects.requireNonNull(messagingTemplate, "messagingTemplate no puede ser null");
        this.securityValidator = Objects.requireNonNull(securityValidator, "securityValidator no puede ser null");
        this.driverGeolocationPort = Objects.requireNonNull(driverGeolocationPort, "driverGeolocationPort no puede ser null");
    }

    /**
     * Procesa la telemetría periódica GPS del repartidor (cada 3 segundos).
     *
     * <p>Destino de escucha STOMP: {@code /app/driver/location}</p>
     *
     * @param message payload con coordenadas GPS (latitud, longitud, driverId, orderId)
     */
    @MessageMapping("/driver/location")
    public void handleDriverLocation(@Payload @Valid DriverLocationMessage message) {

        log.debug("📍 [GPS-TELEMETRY] Coordenadas recibidas cada 3s: {}", message);

        // ── 1. Validación de Seguridad (driver asignado y pedido activo) ──
        ValidationResult validation = securityValidator.validate(
                message.getDriverId(), message.getOrderId());

        if (validation.isRejected()) {
            log.warn("⛔ [GPS-TELEMETRY] Coordenadas rechazadas para driver={}, order={}: {}",
                    message.getDriverId(), message.getOrderId(),
                    validation.getRejectionReason());

            messagingTemplate.convertAndSend(
                    ERROR_QUEUE,
                    new ErrorPayload("TRACKING_UNAUTHORIZED", validation.getRejectionReason()));
            return;
        }

        // ── 2. Persistencia espacial PostGIS (Hot-path + Historial) ────────
        try {
            driverGeolocationPort.updateDriverLocation(
                    message.getDriverId(),
                    new GeoLocation(message.getLatitude(), message.getLongitude()),
                    null, // heading opcional
                    null  // speed opcional
            );
        } catch (Exception ex) {
            log.warn("⚠️ [POSTGIS] Error al actualizar posición GPS de driver={}: {}",
                    message.getDriverId(), ex.getMessage());
        }

        // ── 3. Conversión a evento de tracking con timestamp ─────────────
        DriverLocationUpdate locationUpdate = DriverLocationUpdate.from(message);

        // ── 4. Retransmisión al canal /topic/orders/{orderId}/tracking ────
        String destinationTopic = ORDERS_TRACKING_TOPIC_PREFIX + message.getOrderId() + ORDERS_TRACKING_TOPIC_SUFFIX;
        messagingTemplate.convertAndSend(destinationTopic, locationUpdate);

        // Broadcast a canal de compatibilidad /topic/tracking/{orderId}
        String legacyDestination = LEGACY_TRACKING_TOPIC_PREFIX + message.getOrderId();
        messagingTemplate.convertAndSend(legacyDestination, locationUpdate);

        log.debug("📡 [GPS-TELEMETRY] Retransmitido exitosamente a {}", destinationTopic);
    }

    /**
     * Payload de respuesta ante errores de autorización en telemetría.
     */
    public record ErrorPayload(String code, String message) {}
}
