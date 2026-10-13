package com.delivery.domain.port.input;

import com.delivery.domain.model.Driver;
import com.delivery.domain.port.input.command.AssignNearestDriverCommand;

import java.util.Optional;

/**
 * Puerto de Entrada — Caso de uso: Asignar Repartidor Más Cercano.
 *
 * <p>Define el contrato para la asignación inteligente de un repartidor
 * a un pedido, basada en proximidad geoespacial (PostGIS) y prioridad
 * multinivel.</p>
 *
 * <h3>Algoritmo de asignación:</h3>
 * <ol>
 *   <li>Recuperar el pedido y su punto de recogida (pickup location)</li>
 *   <li>Buscar repartidores activos dentro del radio vía
 *       {@link com.delivery.domain.port.output.DriverGeolocationPort}</li>
 *   <li>Ordenar por prioridad multinivel:
 *       <ul>
 *         <li>Nivel superior primero (MASTER &gt; SENIOR &gt; JUNIOR)</li>
 *         <li>A igual nivel, menor distancia</li>
 *       </ul>
 *   </li>
 *   <li>Asignar el mejor candidato al pedido</li>
 *   <li>Persistir la asignación</li>
 *   <li>Notificar al cliente que un repartidor fue asignado</li>
 * </ol>
 *
 * <h3>Resultado:</h3>
 * <ul>
 *   <li>{@link Optional#of(Object)} — repartidor asignado exitosamente</li>
 *   <li>{@link Optional#empty()} — no se encontraron repartidores
 *       disponibles dentro del radio especificado</li>
 * </ul>
 *
 * <h3>Excepciones esperadas:</h3>
 * <ul>
 *   <li>{@link com.delivery.domain.exception.OrderNotFoundException} — pedido no existe</li>
 *   <li>{@link IllegalStateException} — pedido en estado terminal o sin ubicación de recogida</li>
 * </ul>
 *
 * @see AssignNearestDriverCommand
 * @see Driver
 * @since 1.0.0
 */
public interface AssignNearestDriverUseCase {

    /**
     * Busca y asigna el repartidor más cercano y de mayor prioridad
     * al pedido indicado.
     *
     * @param command contiene el ID del pedido y el radio de búsqueda en km
     * @return repartidor asignado, o {@link Optional#empty()} si no hay
     *         candidatos disponibles
     */
    Optional<Driver> execute(AssignNearestDriverCommand command);
}
