package com.delivery.modules.client.service;

import com.delivery.domain.model.GeoLocation;
import com.delivery.domain.port.output.DriverLiveLocationPort;
import com.delivery.infrastructure.adapter.output.persistence.entity.DriverEntity;
import com.delivery.infrastructure.adapter.output.persistence.entity.MerchantEntity;
import com.delivery.infrastructure.adapter.output.persistence.entity.OrderEntity;
import com.delivery.infrastructure.adapter.output.persistence.entity.UserEntity;
import com.delivery.infrastructure.adapter.output.persistence.repository.DriverJpaRepository;
import com.delivery.infrastructure.adapter.output.persistence.repository.MerchantJpaRepository;
import com.delivery.infrastructure.adapter.output.persistence.repository.OrderJpaRepository;
import com.delivery.infrastructure.adapter.output.persistence.repository.UserJpaRepository;
import com.delivery.modules.client.dto.OrderTrackingResponseDTO;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio de Spring Boot para el Módulo CLIENT (Tracking & PIN OTP).
 *
 * <p>Expone el seguimiento de pedido en tiempo real, recupera las coordenadas
 * volátiles desde Redis (TTL 15s), el nombre real del comercio desde la tabla
 * {@code merchants} y los datos del repartidor desde {@code drivers} + {@code users}.</p>
 *
 * @since 1.0.0
 */
@Service
public class ClientTrackingService {

    private final OrderJpaRepository     orderRepository;
    private final MerchantJpaRepository  merchantRepository;
    private final DriverJpaRepository    driverRepository;
    private final UserJpaRepository      userRepository;
    private final DriverLiveLocationPort driverLiveLocationPort;

    public ClientTrackingService(OrderJpaRepository orderRepository,
                                 MerchantJpaRepository merchantRepository,
                                 DriverJpaRepository driverRepository,
                                 UserJpaRepository userRepository,
                                 DriverLiveLocationPort driverLiveLocationPort) {
        this.orderRepository        = orderRepository;
        this.merchantRepository     = merchantRepository;
        this.driverRepository       = driverRepository;
        this.userRepository         = userRepository;
        this.driverLiveLocationPort = driverLiveLocationPort;
    }

    public Optional<OrderTrackingResponseDTO> getOrderTracking(UUID orderId) {
        Optional<OrderEntity> opt = orderRepository.findById(orderId);
        if (opt.isEmpty()) {
            return Optional.empty();
        }

        OrderEntity order = opt.get();

        // ── 1. Nombre real del comercio ──────────────────────────────────────
        String merchantName = merchantRepository.findById(order.getMerchantId())
                .map(MerchantEntity::getName)
                .orElse("Comercio Tulcán");

        // ── 2. Datos reales del repartidor ───────────────────────────────────
        String driverName  = null;
        String driverPhone = null;
        String driverLevel = null;

        if (order.getDriverId() != null) {
            Optional<DriverEntity> driverOpt = driverRepository.findById(order.getDriverId());
            if (driverOpt.isPresent()) {
                DriverEntity driver = driverOpt.get();
                driverLevel = driver.getDeliveryLevel();

                // Nombre y teléfono vienen del perfil de usuario vinculado al repartidor
                UserEntity driverUser = driver.getUser();
                if (driverUser != null) {
                    driverName  = driverUser.getFullName();
                    driverPhone = driverUser.getPhone();
                } else {
                    // Fallback: consulta directa por userId si la relación lazy no cargó
                    Optional<UserEntity> userOpt = userRepository.findById(driver.getUserId());
                    if (userOpt.isPresent()) {
                        driverName  = userOpt.get().getFullName();
                        driverPhone = userOpt.get().getPhone();
                    }
                }
            }
        }

        // ── 3. Coordenadas GPS en vivo desde Redis (TTL 15s) ─────────────────
        Double driverLat = null;
        Double driverLon = null;

        if (order.getDriverId() != null) {
            GeoLocation liveLoc = driverLiveLocationPort.findLatestLocation(order.getDriverId());
            if (liveLoc != null) {
                driverLat = liveLoc.getLatitude();
                driverLon = liveLoc.getLongitude();
            }
        }

        return Optional.of(new OrderTrackingResponseDTO(
                order.getId(),
                order.getStatus(),
                order.getDeliveryPinValue(),
                merchantName,
                order.getPickupAddress(),
                order.getDeliveryAddress(),
                driverName,
                driverPhone,
                driverLevel,
                order.getTotalAmount(),
                "USD",
                driverLat,
                driverLon,
                Instant.now()
        ));
    }
}
