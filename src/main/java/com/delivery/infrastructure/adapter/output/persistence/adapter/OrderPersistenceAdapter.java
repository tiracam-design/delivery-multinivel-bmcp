package com.delivery.infrastructure.adapter.output.persistence.adapter;

import com.delivery.domain.enums.OrderStatus;
import com.delivery.domain.exception.OrderNotFoundException;
import com.delivery.domain.model.Order;
import com.delivery.domain.port.output.OrderRepositoryPort;
import com.delivery.infrastructure.adapter.output.persistence.entity.OrderEntity;
import com.delivery.infrastructure.adapter.output.persistence.mapper.OrderPersistenceMapper;
import com.delivery.infrastructure.adapter.output.persistence.repository.OrderJpaRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de salida que implementa el puerto {@link OrderRepositoryPort}
 * usando Spring Data JPA + PostgreSQL/PostGIS.
 *
 * <p>Actúa como puente entre el dominio y la infraestructura de
 * persistencia:</p>
 * <ol>
 *   <li>Recibe modelos de dominio ({@link Order})</li>
 *   <li>Los convierte a entidades JPA via {@link OrderPersistenceMapper}</li>
 *   <li>Delega en {@link OrderJpaRepository} para la persistencia</li>
 *   <li>Reconvierte las entidades JPA a modelos de dominio</li>
 * </ol>
 *
 * @since 1.0.0
 */
@Component
@Transactional(readOnly = true)
public class OrderPersistenceAdapter implements OrderRepositoryPort {

    private final OrderJpaRepository jpaRepository;
    private final OrderPersistenceMapper mapper;

    public OrderPersistenceAdapter(OrderJpaRepository jpaRepository,
                                   OrderPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Order save(Order order) {
        OrderEntity entity = mapper.toEntity(order);
        if (order.getId() != null) {
            jpaRepository.findById(order.getId()).ifPresent(existing -> {
                entity.setItems(existing.getItems());
            });
        }
        OrderEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Order> findById(UUID orderId) {
        return jpaRepository.findById(orderId)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void updateStatus(UUID orderId, OrderStatus newStatus) {
        int updated = jpaRepository.updateStatusById(orderId, newStatus.name());
        if (updated == 0) {
            throw new OrderNotFoundException(orderId);
        }
    }

    @Override
    public List<Order> findByCustomerId(UUID customerId) {
        return jpaRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Order> findByDriverIdAndStatus(UUID driverId, OrderStatus status) {
        return jpaRepository.findByDriverIdAndStatusOrderByCreatedAtAsc(
                        driverId, status.name())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Order> findByStatus(OrderStatus status) {
        return jpaRepository.findByStatusOrderByCreatedAtAsc(status.name())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsById(UUID orderId) {
        return jpaRepository.existsById(orderId);
    }

    @Override
    public Double calculateDistanceKm(com.delivery.domain.model.GeoLocation origin,
                                       com.delivery.domain.model.GeoLocation destination) {
        if (origin == null || destination == null) {
            return null;
        }
        try {
            Double dist = jpaRepository.calculateDistanceKm(
                    origin.getLongitude(), origin.getLatitude(),
                    destination.getLongitude(), destination.getLatitude()
            );
            return dist != null ? dist : origin.distanceKmTo(destination);
        } catch (Exception ex) {
            return origin.distanceKmTo(destination);
        }
    }
}
