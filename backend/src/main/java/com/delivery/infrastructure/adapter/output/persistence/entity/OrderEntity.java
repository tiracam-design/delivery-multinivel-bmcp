package com.delivery.infrastructure.adapter.output.persistence.entity;

import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidad JPA que mapea la tabla {@code orders}.
 *
 * <p>Usa Hibernate Spatial para las columnas geoespaciales PostGIS
 * ({@code pickup_location}, {@code delivery_location}) mapeadas
 * como {@link org.locationtech.jts.geom.Point} con SRID 4326.</p>
 *
 * <p>El PIN anti-fraude se persiste como columnas embebidas en la
 * tabla (no como tabla separada), ya que es un Value Object del
 * agregado Order.</p>
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "orders", schema = "bmcp_app")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "driver_id")
    private UUID driverId;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    // ── Direcciones y Geolocalización ───────────────────────────────

    @Column(name = "pickup_address", nullable = false, length = 500)
    private String pickupAddress;

    @Column(name = "pickup_location", columnDefinition = "geometry(Point,4326)")
    private Point pickupLocation;

    @Column(name = "delivery_address", nullable = false, length = 500)
    private String deliveryAddress;

    @Column(name = "delivery_location", columnDefinition = "geometry(Point,4326)")
    private Point deliveryLocation;

    // ── Estimaciones ────────────────────────────────────────────────

    @Column(name = "estimated_distance_km")
    private Double estimatedDistanceKm;

    @Column(name = "estimated_duration_min")
    private Integer estimatedDurationMinutes;

    // ── Monto ───────────────────────────────────────────────────────

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    // ── PIN anti-fraude (embebido) ──────────────────────────────────

    @Column(name = "delivery_pin_value", length = 4)
    private String deliveryPinValue;

    @Column(name = "delivery_pin_created_at")
    private Instant deliveryPinCreatedAt;

    @Column(name = "delivery_pin_expires_at")
    private Instant deliveryPinExpiresAt;

    @Column(name = "delivery_pin_attempts", nullable = false)
    private int deliveryPinAttempts;

    // ── Metadatos ───────────────────────────────────────────────────

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ── Relaciones ──────────────────────────────────────────────────

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true,
               fetch = FetchType.LAZY)
    private List<OrderItemEntity> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true,
               fetch = FetchType.LAZY)
    @OrderBy("changedAt ASC")
    private List<OrderStatusHistoryEntity> statusHistory = new ArrayList<>();

    // ── JPA Lifecycle ───────────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) this.createdAt = now;
        if (this.updatedAt == null) this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // ── Constructor protegido (JPA) + Getters/Setters ───────────────

    public OrderEntity() {}

    public OrderEntity(UUID id) { this.id = id; }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public UUID getMerchantId() { return merchantId; }
    public void setMerchantId(UUID merchantId) { this.merchantId = merchantId; }

    public UUID getDriverId() { return driverId; }
    public void setDriverId(UUID driverId) { this.driverId = driverId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }

    public Point getPickupLocation() { return pickupLocation; }
    public void setPickupLocation(Point pickupLocation) { this.pickupLocation = pickupLocation; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public Point getDeliveryLocation() { return deliveryLocation; }
    public void setDeliveryLocation(Point deliveryLocation) { this.deliveryLocation = deliveryLocation; }

    public Double getEstimatedDistanceKm() { return estimatedDistanceKm; }
    public void setEstimatedDistanceKm(Double v) { this.estimatedDistanceKm = v; }

    public Integer getEstimatedDurationMinutes() { return estimatedDurationMinutes; }
    public void setEstimatedDurationMinutes(Integer v) { this.estimatedDurationMinutes = v; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getDeliveryPinValue() { return deliveryPinValue; }
    public void setDeliveryPinValue(String v) { this.deliveryPinValue = v; }

    public Instant getDeliveryPinCreatedAt() { return deliveryPinCreatedAt; }
    public void setDeliveryPinCreatedAt(Instant v) { this.deliveryPinCreatedAt = v; }

    public Instant getDeliveryPinExpiresAt() { return deliveryPinExpiresAt; }
    public void setDeliveryPinExpiresAt(Instant v) { this.deliveryPinExpiresAt = v; }

    public int getDeliveryPinAttempts() { return deliveryPinAttempts; }
    public void setDeliveryPinAttempts(int v) { this.deliveryPinAttempts = v; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public List<OrderItemEntity> getItems() { return items; }
    public void setItems(List<OrderItemEntity> items) { this.items = items; }

    public List<OrderStatusHistoryEntity> getStatusHistory() { return statusHistory; }
    public void setStatusHistory(List<OrderStatusHistoryEntity> h) { this.statusHistory = h; }
}
