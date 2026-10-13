package com.delivery.domain.exception;

/**
 * Excepción base de dominio para el Sistema de Delivery Multinivel.
 *
 * <p>Todas las excepciones de negocio extienden de esta clase, lo que
 * permite capturarlas de forma genérica en el {@code @ControllerAdvice}
 * de infraestructura y mapearlas a respuestas HTTP estandarizadas
 * (RFC 7807 — Problem Details).</p>
 *
 * <p><strong>Convención:</strong> cada excepción concreta aporta un
 * {@code code} único (ej. {@code "ORDER.INVALID_TRANSITION"}) que
 * sirve como clave de internacionalización y referencia en logs.</p>
 *
 * @since 1.0.0
 */
public abstract class DomainException extends RuntimeException {

    private final String code;

    /**
     * @param code    código máquina único (ej. {@code "ORDER.INVALID_TRANSITION"})
     * @param message mensaje legible para humanos
     */
    protected DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * @param code    código máquina único
     * @param message mensaje legible
     * @param cause   excepción raíz
     */
    protected DomainException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /**
     * Código máquina de la excepción, útil para i18n y filtrado en logs.
     *
     * @return código único no nulo (ej. {@code "ORDER.NOT_FOUND"})
     */
    public String getCode() {
        return code;
    }
}
