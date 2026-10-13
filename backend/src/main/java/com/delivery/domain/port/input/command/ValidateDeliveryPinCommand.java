package com.delivery.domain.port.input.command;

import java.util.Objects;
import java.util.UUID;

/**
 * Comando inmutable para la verificación del PIN de entrega anti-fraude.
 *
 * @see com.delivery.domain.port.input.ValidateDeliveryPinUseCase
 * @since 1.0.0
 */
public final class ValidateDeliveryPinCommand {

    private final UUID orderId;
    private final String pin;

    public ValidateDeliveryPinCommand(UUID orderId, String pin) {
        this.orderId = Objects.requireNonNull(orderId, "orderId es obligatorio");
        this.pin = Objects.requireNonNull(pin, "pin es obligatorio");
    }

    public UUID getOrderId() { return orderId; }
    public String getPin() { return pin; }

    @Override
    public String toString() {
        return String.format("ValidateDeliveryPinCommand[orderId=%s, pin=****]", orderId);
    }
}
