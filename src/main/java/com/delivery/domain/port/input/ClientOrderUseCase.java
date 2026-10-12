package com.delivery.domain.port.input;

import com.delivery.application.dto.OrderResponseDTO;
import com.delivery.domain.port.input.command.CreateOrderCommand;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de Entrada (Input Port) — Módulo del Cliente.
 *
 * <p>Define el contrato para los casos de uso disponibles para clientes
 * en el Sistema de Delivery Multinivel según la BMCP v1.0.0 y Arquitectura Hexagonal.</p>
 *
 * <h3>Operaciones expuestas:</h3>
 * <ul>
 *   <li>{@link #createOrder(CreateOrderCommand)} — Creación de pedidos con cálculo
 *       geoespacial PostGIS de tarifa de envío, generación de PIN anti-fraude
 *       y notificación al comercio vía WebSocket.</li>
 *   <li>{@link #getClientActiveOrders(UUID)} — Consulta de pedidos activos del cliente.</li>
 * </ul>
 *
 * @see CreateOrderCommand
 * @see OrderResponseDTO
 * @since 1.0.0
 */
public interface ClientOrderUseCase {

    /**
     * Crea un nuevo pedido a partir del comando proporcionado.
     *
     * <p>Calcula el costo de envío con coordenadas PostGIS, genera el PIN OTP
     * de 4 dígitos, persiste el pedido en estado CREATED y notifica al comercio
     * a través de su canal WebSocket.</p>
     *
     * @param command datos validados para la creación del pedido
     * @return DTO con la información completa del pedido creado
     */
    OrderResponseDTO createOrder(CreateOrderCommand command);

    /**
     * Recupera todos los pedidos activos (no terminales) de un cliente específico.
     *
     * @param clientId ID del cliente
     * @return lista de pedidos activos
     */
    List<OrderResponseDTO> getClientActiveOrders(UUID clientId);
}
