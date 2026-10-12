package com.delivery.domain.exception;

import java.util.UUID;

/**
 * Se lanza cuando un repartidor no existe en el sistema.
 *
 * @since 1.0.0
 */
public class DriverNotFoundException extends DomainException {

    private static final String CODE = "DRIVER.NOT_FOUND";

    public DriverNotFoundException(UUID driverId) {
        super(CODE, String.format("Repartidor con ID [%s] no encontrado", driverId));
    }
}
