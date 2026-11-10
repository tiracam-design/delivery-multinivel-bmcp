package com.delivery.infrastructure.adapter.input.rest;

import com.delivery.infrastructure.adapter.input.rest.dto.ProductResponseDTO;
import com.delivery.infrastructure.adapter.output.persistence.entity.MerchantEntity;
import com.delivery.infrastructure.adapter.output.persistence.repository.MerchantJpaRepository;
import com.delivery.infrastructure.adapter.output.persistence.repository.ProductJpaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Adaptador de Entrada REST — Catálogo Público de Comercios y Productos.
 *
 * <p>Endpoints de solo lectura accesibles por cualquier usuario autenticado
 * (incluido ROLE_CLIENT). Expone los comercios activos y sus productos disponibles
 * para que el módulo de checkout del cliente pueda construir el menú dinámicamente.</p>
 *
 * <h3>Ruta base:</h3>
 * {@code /api/v1/catalog}
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping({"/v1/catalog", "/api/v1/catalog"})
@Tag(name = "Catálogo Público", description = "Comercios activos y productos disponibles para clientes")
public class CatalogController {

    private final MerchantJpaRepository merchantRepository;
    private final ProductJpaRepository  productRepository;

    public CatalogController(MerchantJpaRepository merchantRepository,
                             ProductJpaRepository productRepository) {
        this.merchantRepository = merchantRepository;
        this.productRepository  = productRepository;
    }

    /**
     * Lista todos los comercios activos con sus datos básicos para el selector del cliente.
     */
    @GetMapping("/merchants")
    @Operation(summary = "Listar comercios activos disponibles para delivery")
    public ResponseEntity<List<Map<String, Object>>> getActiveMerchants() {
        List<Map<String, Object>> merchants = merchantRepository.findAll().stream()
                .filter(MerchantEntity::isActive)
                .map(m -> {
                    Map<String, Object> dto = new LinkedHashMap<>();
                    dto.put("id",       m.getId());
                    dto.put("name",     m.getName());
                    dto.put("category", m.getCategory() != null ? m.getCategory() : "Restaurante");
                    dto.put("address",  m.getAddress());
                    dto.put("phone",    m.getPhone());
                    dto.put("rating",   m.getRating() != null ? m.getRating() : BigDecimal.valueOf(4.5));
                    dto.put("logoUrl",  m.getLogoUrl());
                    // Coordenadas para el mapa
                    if (m.getLocation() != null) {
                        dto.put("latitude",  m.getLocation().getY());
                        dto.put("longitude", m.getLocation().getX());
                    }
                    return dto;
                })
                .toList();

        return ResponseEntity.ok(merchants);
    }

    /**
     * Lista los productos disponibles de un comercio específico.
     * Solo retorna productos con {@code available = true}.
     */
    @GetMapping("/merchants/{merchantId}/products")
    @Operation(summary = "Listar productos disponibles de un comercio")
    public ResponseEntity<List<ProductResponseDTO>> getMerchantProducts(
            @PathVariable UUID merchantId) {

        List<ProductResponseDTO> products = productRepository
                .findByMerchantIdOrderByNameAsc(merchantId)
                .stream()
                .filter(p -> p.isAvailable())
                .map(ProductResponseDTO::fromEntity)
                .toList();

        return ResponseEntity.ok(products);
    }

    /**
     * Lista todos los productos disponibles de todos los comercios.
     * Útil como fallback cuando no se selecciona un comercio específico.
     */
    @GetMapping("/products")
    @Operation(summary = "Listar todos los productos disponibles")
    public ResponseEntity<List<ProductResponseDTO>> getAllAvailableProducts() {
        List<ProductResponseDTO> products = productRepository.findAll()
                .stream()
                .filter(p -> p.isAvailable())
                .map(ProductResponseDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(products);
    }
}
