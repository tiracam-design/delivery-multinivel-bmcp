package com.delivery.domain.exception;

import java.util.UUID;

/**
 * Se lanza cuando un comercio no existe en el sistema.
 *
 * @since 1.0.0
 */
public class MerchantNotFoundException extends DomainException {

    private static final String CODE = "MERCHANT.NOT_FOUND";

    public MerchantNotFoundException(UUID merchantId) {
        super(CODE, String.format("Comercio con ID [%s] no encontrado", merchantId));
    }
}
