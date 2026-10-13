package com.delivery.domain.port.input;

import java.util.UUID;

/**
 * Puerto de Entrada (Input Port) — Caso de uso: Alternar Disponibilidad de Producto.
 *
 * <p>Permite al comercio activar o pausar la disponibilidad (stock) de un producto
 * en su catálogo en tiempo real.</p>
 *
 * @since 1.0.0
 */
public interface ToggleProductStockUseCase {

    /**
     * Alterna la bandera de disponibilidad del producto especificado.
     *
     * @param productId identificador único del producto
     * @return nuevo estado de disponibilidad ({@code true} disponible, {@code false} pausado)
     */
    boolean toggleAvailability(UUID productId);
}
