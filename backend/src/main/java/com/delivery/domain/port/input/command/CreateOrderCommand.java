package com.delivery.domain.port.input.command;

import com.delivery.domain.model.GeoLocation;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Comando inmutable para la creación de un nuevo pedido.
 *
 * <p>Encapsula todos los datos necesarios para invocar el caso de uso
 * {@link com.delivery.domain.port.input.CreateOrderUseCase}. Es un
 * objeto de dominio puro, sin anotaciones de framework.</p>
 *
 * @since 1.0.0
 */
public final class CreateOrderCommand {

    private final UUID customerId;
    private final UUID merchantId;
    private final String pickupAddress;
    private final GeoLocation pickupLocation;
    private final String deliveryAddress;
    private final GeoLocation deliveryLocation;
    private final BigDecimal totalAmount;
    private final String notes;

    public CreateOrderCommand(UUID customerId,
                              UUID merchantId,
                              String pickupAddress,
                              GeoLocation pickupLocation,
                              String deliveryAddress,
                              GeoLocation deliveryLocation,
                              BigDecimal totalAmount,
                              String notes) {
        this.customerId = Objects.requireNonNull(customerId,
                "customerId es obligatorio");
        this.merchantId = Objects.requireNonNull(merchantId,
                "merchantId es obligatorio");
        this.pickupAddress = requireNonBlank(pickupAddress, "pickupAddress");
        this.pickupLocation = pickupLocation;
        this.deliveryAddress = requireNonBlank(deliveryAddress, "deliveryAddress");
        this.deliveryLocation = deliveryLocation;
        this.totalAmount = validateAmount(totalAmount);
        this.notes = notes;
    }

    public CreateOrderCommand(UUID customerId,
                              UUID merchantId,
                              String pickupAddress,
                              Double pickupLatitude,
                              Double pickupLongitude,
                              String deliveryAddress,
                              Double deliveryLatitude,
                              Double deliveryLongitude,
                              BigDecimal totalAmount,
                              String notes) {
        this(customerId, merchantId, pickupAddress,
                (pickupLatitude != null && pickupLongitude != null)
                        ? new GeoLocation(pickupLatitude, pickupLongitude) : null,
                deliveryAddress,
                (deliveryLatitude != null && deliveryLongitude != null)
                        ? new GeoLocation(deliveryLatitude, deliveryLongitude) : null,
                totalAmount, notes);
    }

    public UUID getCustomerId() { return customerId; }
    public UUID getMerchantId() { return merchantId; }
    public String getPickupAddress() { return pickupAddress; }
    public GeoLocation getPickupLocation() { return pickupLocation; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public GeoLocation getDeliveryLocation() { return deliveryLocation; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getNotes() { return notes; }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " es obligatorio y no puede estar vacío");
        }
        return value.strip();
    }

    private static BigDecimal validateAmount(BigDecimal amount) {
        Objects.requireNonNull(amount, "totalAmount es obligatorio");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "totalAmount debe ser positivo, recibido: " + amount);
        }
        return amount;
    }

    @Override
    public String toString() {
        return String.format(
                "CreateOrderCommand[customer=%s, merchant=%s, pickup='%s', amount=%s]",
                customerId, merchantId, pickupAddress, totalAmount);
    }
}
