package com.delivery.application.service;

import com.delivery.domain.model.Product;
import com.delivery.domain.port.input.ToggleProductStockUseCase;
import com.delivery.domain.port.output.ProductRepositoryPort;

import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Implementación del caso de uso: Alternar Disponibilidad de Producto.
 *
 * <p>Actualiza de forma atómica el stock/disponibilidad de un producto
 * en el catálogo del comercio.</p>
 *
 * @see ToggleProductStockUseCase
 * @since 1.0.0
 */
public class ToggleProductStockService implements ToggleProductStockUseCase {

    private static final Logger LOG = Logger.getLogger(ToggleProductStockService.class.getName());

    private final ProductRepositoryPort productRepository;

    public ToggleProductStockService(ProductRepositoryPort productRepository) {
        this.productRepository = Objects.requireNonNull(productRepository,
                "productRepository no puede ser null");
    }

    @Override
    public boolean toggleAvailability(UUID productId) {
        Objects.requireNonNull(productId, "productId no puede ser null");

        LOG.info(() -> String.format("[MERCHANT] Alternando disponibilidad para productId=%s", productId));

        // Intenta actualización atómica en el puerto de persistencia
        try {
            boolean newAvailability = productRepository.toggleAvailability(productId);
            LOG.info(() -> String.format(
                    "[MERCHANT] Disponibilidad de producto %s actualizada a: %s",
                    productId, newAvailability ? "DISPONIBLE" : "PAUSADO"));
            return newAvailability;
        } catch (Exception ex) {
            // Fallback: Recuperar modelo de dominio, alternar y guardar
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + productId));

            boolean updatedStatus = product.toggleAvailability();
            productRepository.save(product);

            LOG.info(() -> String.format(
                    "[MERCHANT] Disponibilidad de producto %s actualizada vía modelo de dominio a: %s",
                    productId, updatedStatus ? "DISPONIBLE" : "PAUSADO"));
            return updatedStatus;
        }
    }
}
