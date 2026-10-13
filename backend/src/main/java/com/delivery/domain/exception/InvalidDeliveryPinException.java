package com.delivery.domain.exception;

/**
 * Se lanza cuando el PIN de entrega (código OTP de 4 dígitos) es
 * inválido: formato incorrecto o no coincide con el esperado.
 *
 * <p>El PIN de entrega es el mecanismo anti-fraude que garantiza
 * la entrega al destinatario correcto. El repartidor debe solicitar
 * el PIN al cliente y verificarlo antes de confirmar la entrega.</p>
 *
 * @see com.delivery.domain.model.DeliveryPin
 * @since 1.0.0
 */
public class InvalidDeliveryPinException extends DomainException {

    private static final String CODE = "ORDER.INVALID_DELIVERY_PIN";

    /**
     * @param reason descripción del motivo de invalidez
     */
    public InvalidDeliveryPinException(String reason) {
        super(CODE, "PIN de entrega inválido: " + reason);
    }
}
