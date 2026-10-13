package com.delivery.modules.client.service;

import com.delivery.domain.model.GeoLocation;
import com.delivery.domain.port.output.DriverLiveLocationPort;
import com.delivery.infrastructure.adapter.output.persistence.entity.OrderEntity;
import com.delivery.infrastructure.adapter.output.persistence.repository.OrderJpaRepository;
import com.delivery.modules.client.dto.OrderTrackingResponseDTO;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio de Spring Boot para el Módulo CLIENT (Tracking & PIN OTP).
 *
 * <p>Expone el seguimiento de pedido en tiempo real, recupera las coordenadas
 * volátiles desde Redis (TTL 15s) y provee el PIN OTP de entrega.</p>
 */
@Service
public class ClientTrackingService {

    private final OrderJpaRepository orderJpaRepository;
    private final DriverLiveLocationPort driverLiveLocationPort;

    public ClientTrackingService(OrderJpaRepository orderJpaRepository,
                                 DriverLiveLocationPort driverLiveLocationPort) {
        this.orderJpaRepository = orderJpaRepository;
        this.driverLiveLocationPort = driverLiveLocationPort;
    }

    public Optional<OrderTrackingResponseDTO> getOrderTracking(UUID orderId) {
        Optional<OrderEntity> opt = orderJpaRepository.findById(orderId);
        if (opt.isEmpty()) {
            return Optional.empty();
        }

        OrderEntity o = opt.get();
        Double driverLat = 0.8080;
        Double driverLon = -77.7205;

        if (o.getDriverId() != null) {
            GeoLocation liveLoc = driverLiveLocationPort.findLatestLocation(o.getDriverId());
            if (liveLoc != null) {
                driverLat = liveLoc.getLatitude();
                driverLon = liveLoc.getLongitude();
            }
        }

        return Optional.of(new OrderTrackingResponseDTO(
                o.getId(),
                o.getStatus(),
                o.getDeliveryPinValue(),
                "Asadero El Leñador (Tulcán)",
                o.getPickupAddress(),
                o.getDeliveryAddress(),
                "Carlos Mendoza Revelo",
                "+593 99 123 4567",
                "SENIOR",
                o.getTotalAmount(),
                "USD",
                driverLat,
                driverLon,
                Instant.now()
        ));
    }
}
