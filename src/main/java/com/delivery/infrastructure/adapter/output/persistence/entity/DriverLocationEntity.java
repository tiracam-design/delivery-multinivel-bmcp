package com.delivery.infrastructure.adapter.output.persistence.entity;

import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad JPA que mapea la tabla {@code driver_locations}.
 * Registros de tracking GPS en tiempo real de repartidores.
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "driver_locations")
public class DriverLocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "driver_id", nullable = false)
    private UUID driverId;

    @Column(name = "location", nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point location;

    @Column(name = "heading")
    private Double heading;

    @Column(name = "speed")
    private Double speed;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @PrePersist
    protected void onCreate() {
        if (this.recordedAt == null) this.recordedAt = Instant.now();
    }

    public DriverLocationEntity() {}

    // ── Getters / Setters ───────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getDriverId() { return driverId; }
    public void setDriverId(UUID driverId) { this.driverId = driverId; }

    public Point getLocation() { return location; }
    public void setLocation(Point location) { this.location = location; }

    public Double getHeading() { return heading; }
    public void setHeading(Double heading) { this.heading = heading; }

    public Double getSpeed() { return speed; }
    public void setSpeed(Double speed) { this.speed = speed; }

    public Instant getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Instant recordedAt) { this.recordedAt = recordedAt; }
}
