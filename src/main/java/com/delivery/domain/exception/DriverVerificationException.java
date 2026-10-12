package com.delivery.domain.exception;

/**
 * Se lanza cuando un repartidor no cumple con los requisitos de verificación
 * documental (Licencia de Conducir o RUC/tax_id) según REQ-SEC-002 de la BMCP.
 *
 * @since 1.0.0
 */
public class DriverVerificationException extends DomainException {

    private static final String CODE = "DRIVER.VERIFICATION_FAILED";

    public DriverVerificationException(String message) {
        super(CODE, message);
    }
}
