package com.delivery.infrastructure.adapter.input.rest;

import com.delivery.domain.port.input.ToggleProductStockUseCase;
import com.delivery.infrastructure.adapter.input.rest.dto.ProductResponseDTO;
import com.delivery.infrastructure.adapter.input.rest.dto.ProductUpsertRequest;
import com.delivery.infrastructure.adapter.output.persistence.entity.ProductEntity;
import com.delivery.infrastructure.adapter.output.persistence.repository.ProductJpaRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Adaptador de Entrada REST — Gestión Completa de Productos y Menú del Comercio.
 *
 * <p>Expone endpoints para listado, creación, edición, eliminación y cambio de disponibilidad
 * de platos en el módulo de Restaurante / Cocina KDS en Tulcán, Carchi ($ USD).</p>
 *
 * <h3>Ruta base:</h3>
 * {@code /api/v1/merchant/products}
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping({"/v1/merchant/products", "/api/v1/merchant/products"})
@PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')")
@Tag(name = "Módulo Comercio - Productos", description = "Endpoints para la gestión del catálogo de productos y stock")
@SecurityRequirement(name = "bearerAuth")
public class MerchantProductController {

    private static final UUID DEFAULT_MERCHANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final ToggleProductStockUseCase toggleProductStockUseCase;
    private final ProductJpaRepository productJpaRepository;

    public MerchantProductController(ToggleProductStockUseCase toggleProductStockUseCase) {
        this(toggleProductStockUseCase, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public MerchantProductController(ToggleProductStockUseCase toggleProductStockUseCase,
                                     ProductJpaRepository productJpaRepository) {
        this.toggleProductStockUseCase = Objects.requireNonNull(toggleProductStockUseCase,
                "toggleProductStockUseCase no puede ser null");
        this.productJpaRepository = productJpaRepository;
    }

    /**
     * Lista todos los platos o productos del menú del comercio.
     */
    @GetMapping
    @Operation(summary = "Listar catálogo de productos del comercio", description = "Retorna todos los platos del comercio actual con su estado de stock.")
    public ResponseEntity<List<ProductResponseDTO>> getProducts(
            @RequestParam(required = false) UUID merchantId) {
        UUID targetMerchantId = merchantId != null ? merchantId : DEFAULT_MERCHANT_ID;
        List<ProductEntity> entities = productJpaRepository.findByMerchantIdOrderByNameAsc(targetMerchantId);
        
        // Si no hay productos específicos, retornar todos los registrados en base de datos
        if (entities.isEmpty()) {
            entities = productJpaRepository.findAll();
        }

        List<ProductResponseDTO> response = entities.stream()
                .map(ProductResponseDTO::fromEntity)
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * Consulta un plato individual por ID.
     */
    @GetMapping("/{productId}")
    @Operation(summary = "Obtener plato por ID")
    public ResponseEntity<ProductResponseDTO> getProductById(@PathVariable UUID productId) {
        return productJpaRepository.findById(productId)
                .map(ProductResponseDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Crea un nuevo plato o producto en el catálogo.
     */
    @PostMapping
    @Transactional
    @Operation(summary = "Crear nuevo plato en el menú", description = "Inserta un nuevo plato en la base de datos PostgreSQL con su precio en USD e imagen.")
    public ResponseEntity<ProductResponseDTO> createProduct(
            @Valid @RequestBody ProductUpsertRequest request) {

        ProductEntity entity = new ProductEntity();
        entity.setMerchantId(request.merchantId() != null ? request.merchantId() : DEFAULT_MERCHANT_ID);
        entity.setName(request.name().trim());
        entity.setDescription(request.description() != null ? request.description().trim() : "");
        entity.setPrice(request.price());
        entity.setCategory(request.category() != null && !request.category().isBlank() ? request.category().trim() : "Platos Fuertes");
        entity.setImageUrl(request.imageUrl() != null ? request.imageUrl().trim() : "");
        entity.setAvailable(request.available() != null ? request.available() : true);

        ProductEntity saved = productJpaRepository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponseDTO.fromEntity(saved));
    }

    /**
     * Actualiza los datos de un plato existente.
     */
    @PutMapping("/{productId}")
    @Transactional
    @Operation(summary = "Editar datos de un plato existente", description = "Actualiza nombre, precio, categoría, descripción e imagen del plato.")
    public ResponseEntity<ProductResponseDTO> updateProduct(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductUpsertRequest request) {

        return productJpaRepository.findById(productId)
                .map(entity -> {
                    entity.setName(request.name().trim());
                    if (request.description() != null) {
                        entity.setDescription(request.description().trim());
                    }
                    entity.setPrice(request.price());
                    if (request.category() != null && !request.category().isBlank()) {
                        entity.setCategory(request.category().trim());
                    }
                    if (request.imageUrl() != null) {
                        entity.setImageUrl(request.imageUrl().trim());
                    }
                    if (request.available() != null) {
                        entity.setAvailable(request.available());
                    }
                    ProductEntity updated = productJpaRepository.save(entity);
                    return ResponseEntity.ok(ProductResponseDTO.fromEntity(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Elimina un plato del catálogo.
     */
    @DeleteMapping("/{productId}")
    @Transactional
    @Operation(summary = "Eliminar plato del menú", description = "Elimina permanentemente el plato del catálogo del restaurante.")
    public ResponseEntity<?> deleteProduct(@PathVariable UUID productId) {
        if (!productJpaRepository.existsById(productId)) {
            return ResponseEntity.notFound().build();
        }
        productJpaRepository.deleteById(productId);
        return ResponseEntity.ok(Map.of(
                "deleted", true,
                "productId", productId,
                "message", "Plato eliminado exitosamente del catálogo."
        ));
    }

    /**
     * Alterna o establece la disponibilidad (stock activo/pausado) de un producto.
     */
    @PatchMapping(value = {"/{productId}/availability", "/{productId}/status"})
    @Transactional
    @Operation(
            summary = "Alternar o establecer disponibilidad/stock de producto",
            description = "Cambia el estado disponible de un producto (disponible ↔ agotado)."
    )
    public ResponseEntity<Map<String, Object>> toggleAvailability(
            @Parameter(description = "ID del producto", required = true)
            @PathVariable("productId") UUID productId,
            @RequestBody(required = false) Map<String, Object> body) {

        boolean isAvailable;
        if (body != null && body.containsKey("available")) {
            isAvailable = Boolean.parseBoolean(String.valueOf(body.get("available")));
            productJpaRepository.findById(productId).ifPresent(p -> {
                p.setAvailable(isAvailable);
                productJpaRepository.save(p);
            });
        } else {
            isAvailable = toggleProductStockUseCase.toggleAvailability(productId);
        }

        return ResponseEntity.ok(Map.of(
                "productId", productId,
                "available", isAvailable,
                "status", isAvailable ? "AVAILABLE" : "OUT_OF_STOCK",
                "message", isAvailable
                        ? "Producto marcado como disponible para pedidos"
                        : "Producto pausado por falta de stock",
                "updatedAt", Instant.now().toString()
        ));
    }

    /**
     * Sobrecarga de conveniencia para pruebas unitarias y llamadas internas.
     */
    public ResponseEntity<Map<String, Object>> toggleAvailability(UUID productId) {
        return toggleAvailability(productId, null);
    }

    /**
     * Endpoint alternativo POST para compatibilidad con clientes HTTP que no soportan PATCH.
     */
    @PostMapping("/{productId}/toggle-availability")
    @Operation(summary = "Alternar disponibilidad (alias POST)")
    public ResponseEntity<Map<String, Object>> toggleAvailabilityPost(
            @PathVariable("productId") UUID productId) {
        return toggleAvailability(productId, null);
    }
}
