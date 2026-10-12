package com.delivery.domain.port.input;

import java.util.UUID;

/**
 * Puerto de Entrada (Input Port) — Caso de uso: Aceptar Pedido por el Comercio.
 *
 * <p>Responsabilidades de la orquestación:</p>
 * <ol>
 *   <li>Transicionar el pedido al estado {@code IN_PREPARATION}.</li>
 *   <li>Registrar el tiempo estimado de preparación en minutos.</li>
 *   <li>Iniciar la búsqueda geoespacial de repartidores a menos de 3 km vía PostGIS.</li>
 *   <li>Notificar en tiempo real al cliente vía WebSocket la hora estimada de entrega.</li>
 * </ol>
 *
 * @since 1.0.0
 */
public interface AcceptOrderUseCase {

    /**
     * Acepta un pedido pendiente, establece su tiempo de preparación,
     * busca repartidores cercanos e inicia la notificación WebSocket.
     *
     * @param orderId            identificador único del pedido
     * @param preparationMinutes minutos estimados para completar la preparación
     */
    void acceptOrder(UUID orderId, int preparationMinutes);
}
