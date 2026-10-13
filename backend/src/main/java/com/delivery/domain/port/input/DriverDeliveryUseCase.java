package com.delivery.domain.port.input;

import java.util.UUID;

/**
 * Puerto de Entrada — Casos de Uso para el Módulo del Repartidor.
 *
 * <p>Define las operaciones que el repartidor puede ejecutar sobre una orden:
 * <ul>
 *   <li>Aceptar un viaje / orden asignada (cambiando su estado a {@code ACCEPTED}).</li>
 *   <li>Validar el PIN de entrega de 4 dígitos (si es correcto, pasa a {@code DELIVERED}
 *       y libera los fondos/saldo al repartidor).</li>
 * </ul>
 * </p>
 *
 * @since 1.0.0
 */
public interface DriverDeliveryUseCase {

    /**
     * Acepta un viaje/pedido asignado.
     *
     * <p>Asocia al repartidor con el pedido y transiciona su estado a
     * {@link com.delivery.domain.enums.OrderStatus#ACCEPTED}.</p>
     *
     * @param orderId  identificador del pedido
     * @param driverId identificador del repartidor que acepta el viaje
     */
    void acceptTrip(UUID orderId, UUID driverId);

    /**
     * Valida el PIN anti-fraude de 4 dígitos proporcionado por el cliente en el punto de entrega.
     *
     * <p>Si el PIN es correcto, transiciona el pedido a
     * {@link com.delivery.domain.enums.OrderStatus#DELIVERED}, libera el saldo en custodia
     * a la billetera del repartidor y notifica a las partes correspondientes.</p>
     *
     * @param orderId  identificador del pedido
     * @param inputPin PIN de 4 dígitos ingresado por el repartidor
     * @return {@code true} si la entrega y el PIN fueron validados exitosamente
     */
    boolean validateDeliveryPin(UUID orderId, String inputPin);
}
