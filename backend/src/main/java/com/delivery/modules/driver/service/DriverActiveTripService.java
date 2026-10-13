package com.delivery.modules.driver.service;

import com.delivery.domain.port.input.DriverDeliveryUseCase;
import com.delivery.infrastructure.adapter.output.persistence.entity.OrderEntity;
import com.delivery.infrastructure.adapter.output.persistence.repository.OrderJpaRepository;
import com.delivery.modules.driver.dto.ActiveTripResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio de Spring Boot para el Módulo del Repartidor:
 * Gestión del viaje activo, navegación y validación de PIN en Tulcán.
 */
@Service
public class DriverActiveTripService {

    private final OrderJpaRepository orderJpaRepository;
    private final DriverDeliveryUseCase driverDeliveryUseCase;

    public DriverActiveTripService(OrderJpaRepository orderJpaRepository,
                                  DriverDeliveryUseCase driverDeliveryUseCase) {
        this.orderJpaRepository = orderJpaRepository;
        this.driverDeliveryUseCase = driverDeliveryUseCase;
    }

    public Optional<ActiveTripResponseDTO> getActiveTripForDriver(UUID driverId) {
        List<OrderEntity> orders = orderJpaRepository.findByDriverIdAndStatusOrderByCreatedAtAsc(
                driverId, "ON_THE_WAY");
        if (orders.isEmpty()) {
            orders = orderJpaRepository.findByDriverIdAndStatusOrderByCreatedAtAsc(
                    driverId, "ACCEPTED");
        }
        if (orders.isEmpty()) {
            return Optional.empty();
        }

        OrderEntity o = orders.get(0);
        return Optional.of(new ActiveTripResponseDTO(
                o.getId(),
                o.getStatus(),
                "Asadero El Leñador (Tulcán)",
                o.getPickupAddress(),
                o.getDeliveryAddress(),
                "Valeria Narváez",
                "+593 99 491 2384",
                o.getTotalAmount(),
                o.getEstimatedDistanceKm(),
                o.getEstimatedDurationMinutes(),
                o.getNotes(),
                o.getCreatedAt()
        ));
    }

    @Transactional
    public boolean reportIncident(UUID orderId, String incidentReason) {
        return orderJpaRepository.findById(orderId).map(order -> {
            order.setStatus("INCIDENT_PAUSED");
            order.setNotes(order.getNotes() != null
                    ? order.getNotes() + " | Incidente reportado: " + incidentReason
                    : "Incidente reportado: " + incidentReason);
            orderJpaRepository.save(order);
            return true;
        }).orElse(false);
    }

    @Transactional
    public boolean validatePin(UUID orderId, String pin) {
        return driverDeliveryUseCase.validateDeliveryPin(orderId, pin);
    }
}
