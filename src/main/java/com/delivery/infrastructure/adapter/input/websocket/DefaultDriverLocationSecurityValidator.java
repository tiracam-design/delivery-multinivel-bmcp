package com.delivery.infrastructure.adapter.input.websocket;

import com.delivery.domain.model.Order;
import com.delivery.domain.port.output.OrderRepositoryPort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementación predeterminada del validador de seguridad de ubicación de repartidores.
 *
 * @since 1.0.0
 */
@Component
public class DefaultDriverLocationSecurityValidator implements DriverLocationSecurityValidator {

    private static final Logger log = LoggerFactory.getLogger(DefaultDriverLocationSecurityValidator.class);

    private final OrderRepositoryPort orderRepository;

    public DefaultDriverLocationSecurityValidator(OrderRepositoryPort orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public ValidationResult validate(UUID driverId, UUID orderId) {
        if (driverId == null || orderId == null) {
            return ValidationResult.rejected("driverId y orderId son obligatorios");
        }

        Optional<Order> optionalOrder = orderRepository.findById(orderId);

        if (optionalOrder.isEmpty()) {
            log.warn("Tracking rechazado: orden {} no encontrada (driver={})", orderId, driverId);
            return ValidationResult.rejected("Orden no encontrada: " + orderId);
        }

        Order order = optionalOrder.get();

        if (!order.hasDriverAssigned()) {
            log.warn("Tracking rechazado: orden {} sin repartidor asignado (driver={})", orderId, driverId);
            return ValidationResult.rejected("La orden no tiene repartidor asignado");
        }

        if (!order.getDriverId().equals(driverId)) {
            log.warn("Tracking rechazado: driver {} no está asignado a orden {} (asignado={})",
                    driverId, orderId, order.getDriverId());
            return ValidationResult.rejected("El repartidor no está asignado a esta orden");
        }

        if (!order.isActive()) {
            log.warn("Tracking rechazado: orden {} en estado terminal {} (driver={})",
                    orderId, order.getStatus(), driverId);
            return ValidationResult.rejected("La orden está en estado terminal: " + order.getStatus());
        }

        log.debug("Tracking autorizado: driver={}, order={}, status={}",
                driverId, orderId, order.getStatus());
        return ValidationResult.authorized();
    }
}
