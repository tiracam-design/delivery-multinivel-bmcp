package com.delivery.application.dto;

import com.delivery.domain.model.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO de respuesta para operaciones de pedidos del cliente (Input/Output boundary).
 *
 * <p>Desacopla el modelo de dominio de los clientes de la API, encapsulando
 * la información completa del pedido, el costo de envío calculado por PostGIS,
 * y el código deliveryPin generado.</p>
 *
 * @since 1.0.0
 */
public record OrderResponseDTO(
        UUID id,
        UUID customerId,
        UUID merchantId,
        UUID driverId,
        String status,
        String pickupAddress,
        Double pickupLatitude,
        Double pickupLongitude,
        String deliveryAddress,
        Double deliveryLatitude,
        Double deliveryLongitude,
        Double estimatedDistanceKm,
        Integer estimatedDurationMinutes,
        BigDecimal shippingCost,
        BigDecimal totalAmount,
        String deliveryPin,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Factory method para mapear desde el agregado de dominio {@link Order}.
     *
     * @param order        entidad de dominio
     * @param shippingCost costo de envío calculado
     * @return DTO inmutable
     */
    public static OrderResponseDTO fromDomain(Order order, BigDecimal shippingCost) {
        if (order == null) {
            return null;
        }

        Double pickupLat = order.getPickupLocation() != null
                ? order.getPickupLocation().getLatitude() : null;
        Double pickupLon = order.getPickupLocation() != null
                ? order.getPickupLocation().getLongitude() : null;
        Double deliveryLat = order.getDeliveryLocation() != null
                ? order.getDeliveryLocation().getLatitude() : null;
        Double deliveryLon = order.getDeliveryLocation() != null
                ? order.getDeliveryLocation().getLongitude() : null;

        String pinValue = order.getDeliveryPin() != null
                ? order.getDeliveryPin().getValue() : null;

        return new OrderResponseDTO(
                order.getId(),
                order.getCustomerId(),
                order.getMerchantId(),
                order.getDriverId(),
                order.getStatus() != null ? order.getStatus().name() : null,
                order.getPickupAddress(),
                pickupLat,
                pickupLon,
                order.getDeliveryAddress(),
                deliveryLat,
                deliveryLon,
                order.getEstimatedDistanceKm(),
                order.getEstimatedDurationMinutes(),
                shippingCost,
                order.getTotalAmount(),
                pinValue,
                order.getNotes(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    /**
     * Factory method simplificado cuando el costo de envío no se recalcula.
     *
     * @param order entidad de dominio
     * @return DTO inmutable
     */
    public static OrderResponseDTO fromDomain(Order order) {
        return fromDomain(order, null);
    }
}
