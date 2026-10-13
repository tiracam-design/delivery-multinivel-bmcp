package com.delivery.domain.enums;

/**
 * Nivel del repartidor dentro del sistema multinivel.
 *
 * <p>El nivel determina la prioridad de asignación de pedidos,
 * las comisiones y los beneficios disponibles. Los niveles se
 * alcanzan por rendimiento y antigüedad.</p>
 *
 * @since 1.0.0
 */
public enum DeliveryLevel {

    /** Repartidor nuevo — pedidos de baja complejidad */
    JUNIOR,

    /** Repartidor experimentado — pedidos estándar */
    SENIOR,

    /** Repartidor élite — pedidos prioritarios y de alta complejidad */
    MASTER
}
