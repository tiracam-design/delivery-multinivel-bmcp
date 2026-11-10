package com.delivery.modules.driver.service;

import com.delivery.domain.port.input.DriverDeliveryUseCase;
import com.delivery.infrastructure.adapter.output.persistence.entity.DriverEntity;
import com.delivery.infrastructure.adapter.output.persistence.entity.MerchantEntity;
import com.delivery.infrastructure.adapter.output.persistence.entity.OrderEntity;
import com.delivery.infrastructure.adapter.output.persistence.entity.UserEntity;
import com.delivery.infrastructure.adapter.output.persistence.repository.DriverJpaRepository;
import com.delivery.infrastructure.adapter.output.persistence.repository.MerchantJpaRepository;
import com.delivery.infrastructure.adapter.output.persistence.repository.OrderJpaRepository;
import com.delivery.infrastructure.adapter.output.persistence.repository.UserJpaRepository;
import com.delivery.modules.driver.dto.ActiveTripResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio de Spring Boot para el Módulo del Repartidor.
 *
 * <p>Gestión del viaje activo, navegación y validación de PIN OTP.
 * Resuelve el nombre real del comercio desde la tabla {@code merchants}
 * y el nombre del cliente desde la tabla {@code users}.</p>
 *
 * @since 1.0.0
 */
@Service
public class DriverActiveTripService {

    private final OrderJpaRepository    orderRepository;
    private final MerchantJpaRepository merchantRepository;
    private final UserJpaRepository     userRepository;
    private final DriverJpaRepository   driverRepository;
    private final DriverDeliveryUseCase driverDeliveryUseCase;

    public DriverActiveTripService(OrderJpaRepository orderRepository,
                                   MerchantJpaRepository merchantRepository,
                                   UserJpaRepository userRepository,
                                   DriverJpaRepository driverRepository,
                                   DriverDeliveryUseCase driverDeliveryUseCase) {
        this.orderRepository      = orderRepository;
        this.merchantRepository   = merchantRepository;
        this.userRepository       = userRepository;
        this.driverRepository     = driverRepository;
        this.driverDeliveryUseCase = driverDeliveryUseCase;
    }

    public Optional<ActiveTripResponseDTO> getActiveTripForDriver(UUID driverId) {
        // Buscar pedido activo asignado a este repartidor (ON_THE_WAY o ACCEPTED)
        List<OrderEntity> orders = orderRepository.findByDriverIdAndStatusOrderByCreatedAtAsc(
                driverId, "ON_THE_WAY");
        if (orders.isEmpty()) {
            orders = orderRepository.findByDriverIdAndStatusOrderByCreatedAtAsc(
                    driverId, "ACCEPTED");
        }
        if (orders.isEmpty()) {
            return Optional.empty();
        }

        OrderEntity order = orders.get(0);

        // ── 1. Nombre real del comercio ──────────────────────────────────────
        String merchantName = merchantRepository.findById(order.getMerchantId())
                .map(MerchantEntity::getName)
                .orElse("Comercio Tulcán");

        // ── 2. Nombre real y teléfono del cliente ────────────────────────────
        String customerName  = resolveCustomerName(order.getCustomerId());
        String customerPhone = resolveCustomerPhone(order.getCustomerId());

        return Optional.of(new ActiveTripResponseDTO(
                order.getId(),
                order.getStatus(),
                merchantName,
                order.getPickupAddress(),
                order.getDeliveryAddress(),
                customerName,
                customerPhone,
                order.getTotalAmount(),
                order.getEstimatedDistanceKm(),
                order.getEstimatedDurationMinutes(),
                order.getNotes(),
                order.getCreatedAt()
        ));
    }

    /**
     * Resuelve el nombre completo del cliente desde la tabla {@code users}.
     * El {@code customerId} en la orden corresponde directamente al {@code id}
     * de la tabla {@code users} (el cliente es un usuario con ROLE_CLIENT).
     */
    private String resolveCustomerName(UUID customerId) {
        return userRepository.findById(customerId)
                .map(UserEntity::getFullName)
                .orElse("Cliente");
    }

    private String resolveCustomerPhone(UUID customerId) {
        return userRepository.findById(customerId)
                .map(UserEntity::getPhone)
                .orElse(null);
    }

    @Transactional
    public boolean reportIncident(UUID orderId, String incidentReason) {
        return orderRepository.findById(orderId).map(order -> {
            order.setStatus("INCIDENT_PAUSED");
            order.setNotes(order.getNotes() != null
                    ? order.getNotes() + " | Incidente reportado: " + incidentReason
                    : "Incidente reportado: " + incidentReason);
            orderRepository.save(order);
            return true;
        }).orElse(false);
    }

    @Transactional
    public boolean validatePin(UUID orderId, String pin) {
        return driverDeliveryUseCase.validateDeliveryPin(orderId, pin);
    }
}
