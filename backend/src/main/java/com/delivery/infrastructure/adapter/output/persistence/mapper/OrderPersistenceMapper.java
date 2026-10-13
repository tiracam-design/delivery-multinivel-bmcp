package com.delivery.infrastructure.adapter.output.persistence.mapper;

import com.delivery.domain.enums.OrderStatus;
import com.delivery.domain.model.DeliveryPin;
import com.delivery.domain.model.GeoLocation;
import com.delivery.domain.model.Order;
import com.delivery.domain.model.OrderStatusChange;
import com.delivery.infrastructure.adapter.output.persistence.entity.OrderEntity;
import com.delivery.infrastructure.adapter.output.persistence.entity.OrderStatusHistoryEntity;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper manual que convierte entre la entidad JPA {@link OrderEntity}
 * (infraestructura) y el modelo de dominio {@link Order}.
 *
 * <h3>¿Por qué mapper manual en vez de MapStruct?</h3>
 * <ul>
 *   <li>La conversión {@link Point} ↔ {@link GeoLocation} requiere
 *       la {@link GeometryFactory} de JTS con SRID 4326</li>
 *   <li>El {@link DeliveryPin} se persiste como columnas embebidas
 *       (no como una tabla separada), lo que requiere mapeo explícito</li>
 *   <li>El historial de estados se mapea entre entity y Value Object</li>
 *   <li>Control total sobre la inversión de ejes (JTS: lon,lat vs dominio: lat,lon)</li>
 * </ul>
 *
 * <h3>Convención de ejes:</h3>
 * <ul>
 *   <li>JTS/PostGIS {@link Point}: {@code x = longitude, y = latitude}</li>
 *   <li>Dominio {@link GeoLocation}: {@code latitude, longitude}</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Component
public class OrderPersistenceMapper {

    /**
     * GeometryFactory con SRID 4326 (WGS 84).
     * Se reutiliza como singleton thread-safe.
     */
    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    // ════════════════════════════════════════════════════════════════
    //  ENTITY → DOMAIN
    // ════════════════════════════════════════════════════════════════

    /**
     * Convierte una entidad JPA a un modelo de dominio.
     *
     * @param entity entidad JPA (no null)
     * @return modelo de dominio {@link Order}
     */
    public Order toDomain(OrderEntity entity) {
        if (entity == null) return null;

        // Reconstruir DeliveryPin desde columnas embebidas
        DeliveryPin deliveryPin = null;
        if (entity.getDeliveryPinValue() != null) {
            deliveryPin = new DeliveryPin(
                    entity.getDeliveryPinValue(),
                    entity.getDeliveryPinCreatedAt(),
                    entity.getDeliveryPinExpiresAt(),
                    entity.getDeliveryPinAttempts()
            );
        }

        // Mapear historial de estados
        List<OrderStatusChange> statusHistory = new ArrayList<>();
        if (entity.getStatusHistory() != null) {
            for (OrderStatusHistoryEntity h : entity.getStatusHistory()) {
                statusHistory.add(new OrderStatusChange(
                        h.getFromStatus() != null
                                ? OrderStatus.valueOf(h.getFromStatus()) : null,
                        OrderStatus.valueOf(h.getToStatus()),
                        h.getChangedAt(),
                        h.getReason()
                ));
            }
        }

        return new Order(
                entity.getId(),
                entity.getCustomerId(),
                entity.getMerchantId(),
                entity.getDriverId(),
                OrderStatus.valueOf(entity.getStatus()),
                entity.getPickupAddress(),
                toGeoLocation(entity.getPickupLocation()),
                entity.getDeliveryAddress(),
                toGeoLocation(entity.getDeliveryLocation()),
                entity.getEstimatedDistanceKm(),
                entity.getEstimatedDurationMinutes(),
                entity.getTotalAmount(),
                deliveryPin,
                entity.getNotes(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                statusHistory
        );
    }

    // ════════════════════════════════════════════════════════════════
    //  DOMAIN → ENTITY
    // ════════════════════════════════════════════════════════════════

    /**
     * Convierte un modelo de dominio a una entidad JPA.
     *
     * @param order modelo de dominio (no null)
     * @return entidad JPA {@link OrderEntity}
     */
    public OrderEntity toEntity(Order order) {
        if (order == null) return null;

        OrderEntity entity = new OrderEntity();
        entity.setId(order.getId());
        entity.setCustomerId(order.getCustomerId());
        entity.setMerchantId(order.getMerchantId());
        entity.setDriverId(order.getDriverId());
        entity.setStatus(order.getStatus().name());

        entity.setPickupAddress(order.getPickupAddress());
        entity.setPickupLocation(toPoint(order.getPickupLocation()));
        entity.setDeliveryAddress(order.getDeliveryAddress());
        entity.setDeliveryLocation(toPoint(order.getDeliveryLocation()));

        entity.setEstimatedDistanceKm(order.getEstimatedDistanceKm());
        entity.setEstimatedDurationMinutes(order.getEstimatedDurationMinutes());
        entity.setTotalAmount(order.getTotalAmount());

        // Mapear PIN anti-fraude a columnas embebidas
        DeliveryPin pin = order.getDeliveryPin();
        if (pin != null) {
            entity.setDeliveryPinValue(pin.getValue());
            entity.setDeliveryPinCreatedAt(pin.getCreatedAt());
            entity.setDeliveryPinExpiresAt(pin.getExpiresAt());
            entity.setDeliveryPinAttempts(pin.getVerificationAttempts());
        }

        entity.setNotes(order.getNotes());
        entity.setCreatedAt(order.getCreatedAt());
        entity.setUpdatedAt(order.getUpdatedAt());

        // Mapear historial de estados
        List<OrderStatusHistoryEntity> historyEntities = new ArrayList<>();
        for (OrderStatusChange change : order.getStatusHistory()) {
            OrderStatusHistoryEntity h = new OrderStatusHistoryEntity();
            h.setOrder(entity);
            h.setFromStatus(change.getFromStatus() != null
                    ? change.getFromStatus().name() : null);
            h.setToStatus(change.getToStatus().name());
            h.setChangedAt(change.getChangedAt());
            h.setReason(change.getReason());
            historyEntities.add(h);
        }
        entity.setStatusHistory(historyEntities);

        return entity;
    }

    // ════════════════════════════════════════════════════════════════
    //  CONVERSIONES GEOESPACIALES
    // ════════════════════════════════════════════════════════════════

    /**
     * Convierte un {@link Point} de JTS/PostGIS a un {@link GeoLocation}
     * del dominio.
     *
     * <p><strong>Inversión de ejes:</strong>
     * JTS {@code Point.getX()} = longitude,
     * JTS {@code Point.getY()} = latitude.</p>
     *
     * @param point punto JTS (nullable)
     * @return GeoLocation del dominio, o {@code null}
     */
    public GeoLocation toGeoLocation(Point point) {
        if (point == null) return null;
        // Point.getY() = latitude, Point.getX() = longitude
        return new GeoLocation(point.getY(), point.getX());
    }

    /**
     * Convierte un {@link GeoLocation} del dominio a un {@link Point}
     * de JTS con SRID 4326.
     *
     * <p><strong>Inversión de ejes:</strong> la Coordinate de JTS
     * recibe {@code (x=longitude, y=latitude)}.</p>
     *
     * @param location ubicación del dominio (nullable)
     * @return punto JTS con SRID 4326, o {@code null}
     */
    public Point toPoint(GeoLocation location) {
        if (location == null) return null;
        // Coordinate(x=longitude, y=latitude)
        Point point = GEOMETRY_FACTORY.createPoint(
                new Coordinate(location.getLongitude(), location.getLatitude()));
        point.setSRID(4326);
        return point;
    }
}
