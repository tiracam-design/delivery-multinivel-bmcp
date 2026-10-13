package com.delivery.modules.merchant.service;

import com.delivery.domain.port.input.AcceptOrderUseCase;
import com.delivery.domain.port.input.ToggleProductStockUseCase;
import com.delivery.infrastructure.adapter.output.persistence.entity.OrderEntity;
import com.delivery.infrastructure.adapter.output.persistence.repository.OrderJpaRepository;
import com.delivery.modules.merchant.dto.KitchenOrderResponseDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Servicio de Spring Boot para el Módulo del Comercio:
 * Gestión de pedidos de cocina, preparación y stock de platos en Tulcán.
 */
@Service
public class MerchantKitchenService {

    private final OrderJpaRepository orderJpaRepository;
    private final AcceptOrderUseCase acceptOrderUseCase;
    private final ToggleProductStockUseCase toggleProductStockUseCase;

    public MerchantKitchenService(OrderJpaRepository orderJpaRepository,
                                  AcceptOrderUseCase acceptOrderUseCase,
                                  ToggleProductStockUseCase toggleProductStockUseCase) {
        this.orderJpaRepository = orderJpaRepository;
        this.acceptOrderUseCase = acceptOrderUseCase;
        this.toggleProductStockUseCase = toggleProductStockUseCase;
    }

    public List<KitchenOrderResponseDTO> getPendingKitchenOrders() {
        return orderJpaRepository.findByStatusOrderByCreatedAtAsc("CREATED").stream()
                .map(o -> new KitchenOrderResponseDTO(
                        o.getId(),
                        o.getStatus(),
                        "Valeria Narváez (Tulcán)",
                        o.getDeliveryAddress(),
                        o.getTotalAmount(),
                        o.getNotes(),
                        o.getCreatedAt()
                ))
                .toList();
    }

    public void acceptOrder(UUID orderId, int preparationMinutes) {
        acceptOrderUseCase.acceptOrder(orderId, preparationMinutes);
    }

    public boolean toggleProductAvailability(UUID productId) {
        return toggleProductStockUseCase.toggleAvailability(productId);
    }
}
