package com.delivery.domain.port.output;

import java.util.UUID;

/**
 * Puerto de Salida — Control de Expiración de Pedidos en Memoria (Redis).
 *
 * <p>Gestiona claves con TTL para monitorear el tiempo límite que tiene el restaurante
 * para aceptar un nuevo pedido (5 minutos según BMCP v1.0.0).</p>
 *
 * @since 1.0.0
 */
public interface OrderExpirationPort {

    /**
     * Registra un temporizador volátil de 5 minutos en Redis para un pedido recién creado.
     * Si el restaurante no lo acepta a tiempo, Redis Keyspace Notifications emitirá el evento de expiración.
     *
     * @param orderId identificador del pedido
     */
    void scheduleUnacceptedOrderTimeout(UUID orderId);

    /**
     * Cancela el temporizador al momento en que el restaurante acepta el pedido.
     *
     * @param orderId identificador del pedido
     */
    void cancelUnacceptedOrderTimeout(UUID orderId);
}
