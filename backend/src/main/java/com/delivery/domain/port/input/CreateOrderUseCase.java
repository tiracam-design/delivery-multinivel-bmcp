package com.delivery.domain.port.input;

import com.delivery.domain.model.Order;
import com.delivery.domain.port.input.command.CreateOrderCommand;

/**
 * Puerto de Entrada — Caso de uso: Crear Pedido.
 *
 * <p>Define el contrato para la creación de un nuevo pedido en el sistema.
 * Este puerto es implementado por la capa de Application (Service) e
 * invocado por los adaptadores de entrada (REST Controller, WebSocket).</p>
 *
 * <h3>Flujo de ejecución esperado:</h3>
 * <ol>
 *   <li>Validar que el cliente exista y esté activo</li>
 *   <li>Validar que las direcciones estén dentro de zonas de cobertura</li>
 *   <li>Crear la entidad {@link Order} en estado {@code CREATED}</li>
 *   <li>Generar el PIN de entrega anti-fraude (OTP 4 dígitos)</li>
 *   <li>Persistir el pedido vía {@link com.delivery.domain.port.output.OrderRepositoryPort}</li>
 *   <li>Notificar al comercio vía {@link com.delivery.domain.port.output.NotificationPort}</li>
 *   <li>Retornar el pedido creado con su ID asignado</li>
 * </ol>
 *
 * <h3>Excepciones esperadas:</h3>
 * <ul>
 *   <li>{@link IllegalArgumentException} — si los datos del comando son inválidos</li>
 *   <li>{@link com.delivery.domain.exception.DomainException} — si se viola una regla de negocio</li>
 * </ul>
 *
 * @see CreateOrderCommand
 * @see Order
 * @since 1.0.0
 */
public interface CreateOrderUseCase {

    /**
     * Crea un nuevo pedido a partir del comando proporcionado.
     *
     * @param command datos validados para la creación del pedido
     * @return pedido creado con ID asignado y PIN generado
     */
    Order execute(CreateOrderCommand command);
}
