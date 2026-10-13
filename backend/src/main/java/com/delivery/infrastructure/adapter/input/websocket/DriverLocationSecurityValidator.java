package com.delivery.infrastructure.adapter.input.websocket;

import java.util.UUID;

/**
 * Validador de seguridad para mensajes WebSocket de tracking.
 *
 * <p>Garantiza que solo el repartidor asignado a una orden activa pueda
 * publicar eventos de ubicación GPS en el canal de tracking correspondiente.</p>
 *
 * @since 1.0.0
 */
public interface DriverLocationSecurityValidator {

    /**
     * Valida si el repartidor está autorizado para publicar ubicación en la orden.
     *
     * @param driverId ID del repartidor que envía la ubicación
     * @param orderId  ID de la orden a la que se asocia la ubicación
     * @return resultado de la validación
     */
    ValidationResult validate(UUID driverId, UUID orderId);

    /**
     * Resultado inmutable de una validación de seguridad.
     */
    final class ValidationResult {

        private final boolean authorized;
        private final String rejectionReason;

        private ValidationResult(boolean authorized, String rejectionReason) {
            this.authorized = authorized;
            this.rejectionReason = rejectionReason;
        }

        public static ValidationResult authorized() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult rejected(String reason) {
            return new ValidationResult(false, reason);
        }

        public boolean isAuthorized() { return authorized; }
        public boolean isRejected() { return !authorized; }
        public String getRejectionReason() { return rejectionReason; }
    }
}
