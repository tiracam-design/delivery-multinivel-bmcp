package com.delivery.domain.port.output;

import com.delivery.domain.model.Product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de Salida — Repositorio de Productos.
 *
 * <p>Define el contrato para la persistencia y recuperación de productos
 * en el catálogo del comercio.</p>
 *
 * @see Product
 * @since 1.0.0
 */
public interface ProductRepositoryPort {

    Optional<Product> findById(UUID productId);

    Product save(Product product);

    boolean toggleAvailability(UUID productId);

    List<Product> findByMerchantId(UUID merchantId);
}
