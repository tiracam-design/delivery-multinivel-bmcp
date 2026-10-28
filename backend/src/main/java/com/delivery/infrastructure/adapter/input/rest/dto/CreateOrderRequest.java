package com.delivery.infrastructure.adapter.input.rest.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO de entrada para la creación de pedidos en Tulcán, Ecuador ($ USD).
 *
 * <p>Soporta tanto creación directa con monto total como con lista detallada
 * de ítems de menú con cálculo automático de coordenadas PostGIS en Tulcán.</p>
 *
 * @since 1.0.0
 */
public record CreateOrderRequest(

        UUID customerId,

        UUID merchantId,

        String pickupAddress,

        Double pickupLatitude,
        Double pickupLongitude,

        String deliveryAddress,

        Double deliveryLatitude,
        Double deliveryLongitude,

        List<OrderItemRequest> items,

        BigDecimal shippingFee,

        BigDecimal totalAmount,

        String notes,

        String paymentMethod,

        String paymentReference
) {
    public CreateOrderRequest(
            UUID customerId,
            UUID merchantId,
            String pickupAddress,
            Double pickupLatitude,
            Double pickupLongitude,
            String deliveryAddress,
            Double deliveryLatitude,
            Double deliveryLongitude,
            BigDecimal totalAmount,
            String notes
    ) {
        this(customerId, merchantId, pickupAddress, pickupLatitude, pickupLongitude,
             deliveryAddress, deliveryLatitude, deliveryLongitude, null, null, totalAmount, notes, null, null);
    }

    public CreateOrderRequest(
            UUID customerId,
            UUID merchantId,
            String pickupAddress,
            Double pickupLatitude,
            Double pickupLongitude,
            String deliveryAddress,
            Double deliveryLatitude,
            Double deliveryLongitude,
            List<OrderItemRequest> items,
            BigDecimal shippingFee,
            BigDecimal totalAmount,
            String notes
    ) {
        this(customerId, merchantId, pickupAddress, pickupLatitude, pickupLongitude,
             deliveryAddress, deliveryLatitude, deliveryLongitude, items, shippingFee, totalAmount, notes, null, null);
    }

    /**
     * Retorna las notas enriquecidas con la información del método de pago y referencia.
     */
    public String effectiveNotes() {
        if (paymentMethod != null && !paymentMethod.isBlank()) {
            String payRef = (paymentReference != null && !paymentReference.isBlank()) ? paymentReference : "N/A";
            String payInfo = "[PAGO: " + paymentMethod.toUpperCase() + " | REF: " + payRef + "]";
            return notes != null && !notes.isBlank() ? payInfo + " - " + notes : payInfo;
        }
        return notes;
    }

    /**
     * Calcula o retorna las coordenadas de recogida con fallback a Tulcán (Asadero El Leñador).
     */
    public Double effectivePickupLat() {
        return pickupLatitude != null ? pickupLatitude : 0.8116;
    }

    public Double effectivePickupLon() {
        return pickupLongitude != null ? pickupLongitude : -77.7172;
    }

    /**
     * Calcula o retorna las coordenadas de entrega con fallback a Tulcán (Terminal Terrestre).
     */
    public Double effectiveDeliveryLat() {
        return deliveryLatitude != null ? deliveryLatitude : 0.8035;
    }

    public Double effectiveDeliveryLon() {
        return deliveryLongitude != null ? deliveryLongitude : -77.7245;
    }

    /**
     * Calcula el monto total en USD a partir de los ítems y envío si no viene preestablecido.
     */
    public BigDecimal calculateEffectiveTotal() {
        if (totalAmount != null && totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            return totalAmount;
        }

        BigDecimal itemsSum = BigDecimal.ZERO;
        if (items != null) {
            for (OrderItemRequest item : items) {
                if (item.unitPrice() != null && item.quantity() != null) {
                    itemsSum = itemsSum.add(item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())));
                }
            }
        }

        BigDecimal fee = shippingFee != null ? shippingFee : new BigDecimal("1.50");
        BigDecimal calculated = itemsSum.add(fee);
        return calculated.compareTo(BigDecimal.ZERO) > 0 ? calculated : new BigDecimal("12.50");
    }
}
