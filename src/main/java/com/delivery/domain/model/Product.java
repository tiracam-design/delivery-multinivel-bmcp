package com.delivery.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad de dominio que representa un Producto de un comercio en el catálogo.
 *
 * <p>Aplica DDD: POJO puro sin dependencias de frameworks ni anotaciones ORM.</p>
 *
 * @since 1.0.0
 */
public class Product {

    private final UUID id;
    private final UUID merchantId;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private String imageUrl;
    private boolean available;
    private final Instant createdAt;
    private Instant updatedAt;

    public Product(UUID id,
                   UUID merchantId,
                   String name,
                   String description,
                   BigDecimal price,
                   String category,
                   String imageUrl,
                   boolean available,
                   Instant createdAt,
                   Instant updatedAt) {
        this.id = id;
        this.merchantId = Objects.requireNonNull(merchantId, "merchantId es obligatorio");
        this.name = requireNonBlank(name, "name");
        this.description = description;
        this.price = validatePrice(price);
        this.category = category;
        this.imageUrl = imageUrl;
        this.available = available;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    /**
     * Alterna la disponibilidad (stock) del producto.
     *
     * @return nuevo estado de disponibilidad
     */
    public boolean toggleAvailability() {
        this.available = !this.available;
        this.updatedAt = Instant.now();
        return this.available;
    }

    public UUID getId() { return id; }
    public UUID getMerchantId() { return merchantId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public String getCategory() { return category; }
    public String getImageUrl() { return imageUrl; }
    public boolean isAvailable() { return available; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setAvailable(boolean available) {
        this.available = available;
        this.updatedAt = Instant.now();
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " no puede estar vacío");
        }
        return value.strip();
    }

    private static BigDecimal validatePrice(BigDecimal price) {
        Objects.requireNonNull(price, "price es obligatorio");
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor a cero, recibido: " + price);
        }
        return price;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product product)) return false;
        return Objects.equals(id, product.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Product[id=%s, name='%s', available=%s]", id, name, available);
    }
}
