package com.delivery.domain.port.output;

import com.delivery.domain.enums.OrderStatus;
import com.delivery.domain.model.Order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de Salida — Repositorio de Pedidos.
 *
 * <p>Define el contrato que la capa de dominio necesita para persistir
 * y recuperar pedidos. La implementación concreta vive en la capa de
 * infraestructura (JPA Persistence Adapter).</p>
 *
 * @see Order
 * @since 1.0.0
 */
public interface OrderRepositoryPort {

    Order save(Order order);

    Optional<Order> findById(UUID orderId);

    void updateStatus(UUID orderId, OrderStatus newStatus);

    List<Order> findByCustomerId(UUID customerId);

    List<Order> findByDriverIdAndStatus(UUID driverId, OrderStatus status);

    List<Order> findByStatus(OrderStatus status);

    boolean existsById(UUID orderId);

    /**
     * Calcula la distancia geodésica en kilómetros entre dos coordenadas usando PostGIS.
     * Retorna fallback con Haversine si no se sobreescribe.
     */
    default Double calculateDistanceKm(com.delivery.domain.model.GeoLocation origin,
                                       com.delivery.domain.model.GeoLocation destination) {
        if (origin == null || destination == null) {
            return null;
        }
        return origin.distanceKmTo(destination);
    }
}
