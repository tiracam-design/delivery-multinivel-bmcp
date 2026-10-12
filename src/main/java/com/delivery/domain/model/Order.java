package com.delivery.domain.model;

import com.delivery.domain.enums.OrderStatus;
import com.delivery.domain.exception.InvalidDeliveryPinException;
import com.delivery.domain.exception.InvalidStateTransitionException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad raíz del agregado de Pedidos.
 *
 * <p>Encapsula toda la lógica de negocio del ciclo de vida de un pedido
 * en el Sistema de Delivery Multinivel, incluyendo:</p>
 * <ul>
 *   <li><strong>Máquina de estados</strong> — transiciones controladas
 *       vía {@link #transitionTo(OrderStatus)}</li>
 *   <li><strong>Flujo anti-fraude</strong> — verificación de PIN OTP
 *       al momento de la entrega vía {@link #confirmDelivery(String)}</li>
 *   <li><strong>Historial de estados</strong> — auditoría completa
 *       de todas las transiciones</li>
 *   <li><strong>Geolocalización</strong> — puntos de recogida y entrega</li>
 * </ul>
 *
 * <h3>Principios DDD aplicados:</h3>
 * <ul>
 *   <li>POJO puro — sin anotaciones {@code @Entity}, {@code @Component}
 *       ni dependencias de framework</li>
 *   <li>Invariantes protegidas — todo cambio de estado pasa por métodos
 *       de negocio que validan reglas</li>
 *   <li>Value Objects — {@link DeliveryPin}, {@link GeoLocation},
 *       {@link OrderStatusChange}</li>
 *   <li>Encapsulación fuerte — setters prohibidos, mutación solo vía
 *       comportamiento de negocio</li>
 * </ul>
 *
 * @see OrderStatus
 * @see DeliveryPin
 * @since 1.0.0
 */
public class Order {

    // ── Identidad ───────────────────────────────────────────────────

    private UUID id;

    // ── Relaciones ──────────────────────────────────────────────────

    private UUID customerId;
    private UUID merchantId;
    private UUID driverId;

    // ── Estado ──────────────────────────────────────────────────────

    private OrderStatus status;
    private final List<OrderStatusChange> statusHistory;

    // ── Direcciones y Geolocalización ───────────────────────────────

    private String pickupAddress;
    private GeoLocation pickupLocation;
    private String deliveryAddress;
    private GeoLocation deliveryLocation;

    // ── Estimaciones ────────────────────────────────────────────────

    private Double estimatedDistanceKm;
    private Integer estimatedDurationMinutes;

    // ── Montos ──────────────────────────────────────────────────────

    private BigDecimal totalAmount;

    // ── Anti-fraude ─────────────────────────────────────────────────

    private DeliveryPin deliveryPin;

    // ── Metadatos ───────────────────────────────────────────────────

    private String notes;
    private Instant createdAt;
    private Instant updatedAt;

    // ════════════════════════════════════════════════════════════════
    //  CONSTRUCTOR — reconstrucción desde persistencia
    // ════════════════════════════════════════════════════════════════

    /**
     * Constructor completo para reconstrucción desde persistencia.
     * Para crear nuevos pedidos usar {@link #create}.
     */
    public Order(UUID id,
                 UUID customerId,
                 UUID merchantId,
                 UUID driverId,
                 OrderStatus status,
                 String pickupAddress,
                 GeoLocation pickupLocation,
                 String deliveryAddress,
                 GeoLocation deliveryLocation,
                 Double estimatedDistanceKm,
                 Integer estimatedDurationMinutes,
                 BigDecimal totalAmount,
                 DeliveryPin deliveryPin,
                 String notes,
                 Instant createdAt,
                 Instant updatedAt,
                 List<OrderStatusChange> statusHistory) {
        this.id = id;
        this.customerId = customerId;
        this.merchantId = merchantId;
        this.driverId = driverId;
        this.status = status;
        this.pickupAddress = pickupAddress;
        this.pickupLocation = pickupLocation;
        this.deliveryAddress = deliveryAddress;
        this.deliveryLocation = deliveryLocation;
        this.estimatedDistanceKm = estimatedDistanceKm;
        this.estimatedDurationMinutes = estimatedDurationMinutes;
        this.totalAmount = totalAmount;
        this.deliveryPin = deliveryPin;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.statusHistory = statusHistory != null
                ? new ArrayList<>(statusHistory)
                : new ArrayList<>();
    }

    // ════════════════════════════════════════════════════════════════
    //  FACTORY METHOD — creación de nuevos pedidos
    // ════════════════════════════════════════════════════════════════

    /**
     * Crea un nuevo pedido en estado {@link OrderStatus#CREATED}.
     *
     * <p>Genera automáticamente:</p>
     * <ul>
     *   <li>PIN de entrega anti-fraude ({@link DeliveryPin#generate()})</li>
     *   <li>Timestamps de creación y actualización</li>
     *   <li>Distancia estimada entre puntos (si ambas ubicaciones están presentes)</li>
     *   <li>Primer registro en el historial de estados</li>
     * </ul>
     *
     * @param customerId      ID del cliente que realiza el pedido
     * @param merchantId      ID del comercio que recibe el pedido
     * @param pickupAddress   dirección de recogida (texto)
     * @param pickupLocation  coordenadas de recogida (nullable)
     * @param deliveryAddress dirección de entrega (texto)
     * @param deliveryLocation coordenadas de entrega (nullable)
     * @param totalAmount     monto total del pedido (debe ser positivo)
     * @param notes           notas adicionales (nullable)
     * @return nuevo pedido en estado {@code CREATED}
     * @throws IllegalArgumentException si los datos obligatorios son inválidos
     */
    public static Order create(UUID customerId,
                               UUID merchantId,
                               String pickupAddress,
                               GeoLocation pickupLocation,
                               String deliveryAddress,
                               GeoLocation deliveryLocation,
                               BigDecimal totalAmount,
                               String notes) {

        Objects.requireNonNull(customerId, "customerId es obligatorio");
        Objects.requireNonNull(merchantId, "merchantId es obligatorio");
        requireNonBlank(pickupAddress, "pickupAddress");
        requireNonBlank(deliveryAddress, "deliveryAddress");

        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "totalAmount debe ser positivo, recibido: " + totalAmount);
        }

        Instant now = Instant.now();
        DeliveryPin pin = DeliveryPin.generate();

        Double estimatedDistance = null;
        if (pickupLocation != null && deliveryLocation != null) {
            estimatedDistance = pickupLocation.distanceKmTo(deliveryLocation);
        }

        List<OrderStatusChange> history = new ArrayList<>();
        history.add(new OrderStatusChange(null, OrderStatus.CREATED, now,
                "Pedido creado por el cliente"));

        return new Order(
                null,               // id — asignado por persistencia
                customerId,
                merchantId,
                null,               // driverId — asignado al aceptar
                OrderStatus.CREATED,
                pickupAddress,
                pickupLocation,
                deliveryAddress,
                deliveryLocation,
                estimatedDistance,
                null,               // estimatedDurationMinutes
                totalAmount,
                pin,
                notes,
                now,
                now,
                history
        );
    }

    // ════════════════════════════════════════════════════════════════
    //  MÁQUINA DE ESTADOS
    // ════════════════════════════════════════════════════════════════

    /** Transición de estado sin motivo. */
    public void transitionTo(OrderStatus nextStatus) {
        transitionTo(nextStatus, null);
    }

    /**
     * Transición de estado con motivo de auditoría.
     * Prohíbe {@code DELIVERED} directo — usar {@link #confirmDelivery}.
     */
    public void transitionTo(OrderStatus nextStatus, String reason) {
        Objects.requireNonNull(nextStatus, "El estado destino no puede ser null");

        if (nextStatus == OrderStatus.DELIVERED) {
            throw new InvalidStateTransitionException(this.status, nextStatus);
        }

        validateTransition(nextStatus);
        applyTransition(nextStatus, reason);
    }

    /**
     * Confirma la entrega verificando el PIN anti-fraude OTP.
     * Único camino para alcanzar {@code DELIVERED}.
     *
     * @param candidatePin PIN de 4 dígitos del repartidor
     * @throws InvalidStateTransitionException si no está en ON_THE_WAY
     * @throws InvalidDeliveryPinException si el PIN es inválido
     */
    public void confirmDelivery(String candidatePin) {
        validateTransition(OrderStatus.DELIVERED);

        if (this.deliveryPin == null) {
            throw new InvalidDeliveryPinException(
                    "El pedido no tiene un PIN de entrega asignado");
        }

        boolean pinValid = this.deliveryPin.verify(candidatePin);
        if (!pinValid) {
            throw new InvalidDeliveryPinException(
                    String.format("El PIN proporcionado no coincide. "
                            + "Intentos restantes: %d",
                            deliveryPin.getRemainingAttempts()));
        }

        applyTransition(OrderStatus.DELIVERED,
                "Entrega confirmada con PIN anti-fraude verificado");
    }

    // ── Métodos de negocio auxiliares ────────────────────────────────

    /**
     * Asigna un repartidor al pedido.
     * @throws IllegalStateException si el pedido está en estado terminal
     */
    public void assignDriver(UUID driverId) {
        Objects.requireNonNull(driverId, "driverId no puede ser null");
        if (this.status.isTerminal()) {
            throw new IllegalStateException(
                    "No se puede asignar repartidor en estado terminal: " + this.status);
        }
        this.driverId = driverId;
        this.updatedAt = Instant.now();
    }

    /**
     * Regenera el PIN de entrega.
     * @throws IllegalStateException si el pedido está en estado terminal
     */
    public void regenerateDeliveryPin() {
        if (this.status.isTerminal()) {
            throw new IllegalStateException(
                    "No se puede regenerar PIN en estado terminal: " + this.status);
        }
        this.deliveryPin = DeliveryPin.generate();
        this.updatedAt = Instant.now();
    }

    /**
     * Establece el tiempo estimado de preparación acordado por el comercio.
     *
     * @param preparationMinutes minutos de preparación (debe ser mayor a cero)
     */
    public void setPreparationTime(int preparationMinutes) {
        if (preparationMinutes <= 0) {
            throw new IllegalArgumentException(
                    "El tiempo de preparación debe ser mayor a 0 minutos, recibido: " + preparationMinutes);
        }
        if (this.status.isTerminal()) {
            throw new IllegalStateException(
                    "No se puede asignar tiempo de preparación en estado terminal: " + this.status);
        }
        this.estimatedDurationMinutes = preparationMinutes;
        this.updatedAt = Instant.now();
    }

    // ── Métodos privados ────────────────────────────────────────────

    private void validateTransition(OrderStatus nextStatus) {
        if (!this.status.canTransitionTo(nextStatus)) {
            throw new InvalidStateTransitionException(this.status, nextStatus);
        }
    }

    private void applyTransition(OrderStatus nextStatus, String reason) {
        OrderStatus previousStatus = this.status;
        this.status = nextStatus;
        this.updatedAt = Instant.now();
        this.statusHistory.add(new OrderStatusChange(
                previousStatus, nextStatus, this.updatedAt, reason));
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " es obligatorio y no puede estar vacío");
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  CONSULTAS DE ESTADO
    // ════════════════════════════════════════════════════════════════

    public boolean isCancellable() {
        return this.status.canTransitionTo(OrderStatus.CANCELLED);
    }

    public boolean isActive() {
        return this.status.isActive();
    }

    public boolean isDelivered() {
        return this.status == OrderStatus.DELIVERED;
    }

    public boolean isCancelled() {
        return this.status == OrderStatus.CANCELLED;
    }

    public boolean hasDriverAssigned() {
        return this.driverId != null;
    }

    // ════════════════════════════════════════════════════════════════
    //  GETTERS (sin setters)
    // ════════════════════════════════════════════════════════════════

    public UUID getId() { return id; }
    public UUID getCustomerId() { return customerId; }
    public UUID getMerchantId() { return merchantId; }
    public UUID getDriverId() { return driverId; }
    public OrderStatus getStatus() { return status; }
    public String getPickupAddress() { return pickupAddress; }
    public GeoLocation getPickupLocation() { return pickupLocation; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public GeoLocation getDeliveryLocation() { return deliveryLocation; }
    public Double getEstimatedDistanceKm() { return estimatedDistanceKm; }
    public Integer getEstimatedDurationMinutes() { return estimatedDurationMinutes; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public DeliveryPin getDeliveryPin() { return deliveryPin; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public List<OrderStatusChange> getStatusHistory() {
        return Collections.unmodifiableList(statusHistory);
    }

    // ════════════════════════════════════════════════════════════════
    //  EQUALS / HASHCODE / TOSTRING
    // ════════════════════════════════════════════════════════════════

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order order)) return false;
        return id != null && Objects.equals(id, order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Order[id=%s, status=%s, customer=%s, driver=%s, amount=%s]",
                id, status, customerId, driverId, totalAmount);
    }
}
