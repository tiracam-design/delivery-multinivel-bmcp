package com.delivery.infrastructure.config;

import com.delivery.application.service.AssignNearestDriverService;
import com.delivery.application.service.CreateOrderService;
import com.delivery.application.service.ValidateDeliveryPinService;
import com.delivery.domain.port.input.AssignNearestDriverUseCase;
import com.delivery.domain.port.input.CreateOrderUseCase;
import com.delivery.domain.port.input.ValidateDeliveryPinUseCase;
import com.delivery.domain.port.output.DriverGeolocationPort;
import com.delivery.domain.port.output.NotificationPort;
import com.delivery.domain.port.output.OrderRepositoryPort;
import com.delivery.domain.port.output.PinRateLimiterPort;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de Spring que registra los beans de la capa de Application.
 *
 * <p>Los Services de application no llevan {@code @Service} directamente
 * porque son clases de dominio/application sin dependencias de Spring.
 * Este {@code @Configuration} los registra como beans inyectándoles
 * los Output Ports (implementados por los adaptadores de infraestructura).</p>
 *
 * @since 1.0.0
 */
@Configuration
public class UseCaseConfig {

    /**
     * Registra el caso de uso de creación de pedidos.
     *
     * @param orderRepository  adaptador de persistencia (inyectado por Spring)
     * @param notificationPort adaptador de notificaciones (inyectado por Spring)
     * @return instancia del servicio de creación de pedidos
     */
    @Bean
    public CreateOrderUseCase createOrderUseCase(
            OrderRepositoryPort orderRepository,
            NotificationPort notificationPort) {
        return new CreateOrderService(orderRepository, notificationPort);
    }

    /**
     * Registra el caso de uso de validación de PIN anti-fraude con Rate Limiting en Redis.
     *
     * @param orderRepository    adaptador de persistencia (inyectado por Spring)
     * @param notificationPort   adaptador de notificaciones (inyectado por Spring)
     * @param pinRateLimiterPort adaptador de rate limiting con Redis
     * @return instancia del servicio de validación de PIN
     */
    @Bean
    public ValidateDeliveryPinUseCase validateDeliveryPinUseCase(
            OrderRepositoryPort orderRepository,
            NotificationPort notificationPort,
            PinRateLimiterPort pinRateLimiterPort,
            com.delivery.domain.port.output.FinancialSettlementPort financialSettlementPort) {
        return new ValidateDeliveryPinService(orderRepository, notificationPort, pinRateLimiterPort, financialSettlementPort);
    }

    /**
     * Registra el caso de uso de asignación del repartidor más cercano.
     *
     * @param orderRepository      adaptador de persistencia (inyectado por Spring)
     * @param driverGeolocationPort adaptador geoespacial PostGIS (inyectado por Spring)
     * @param notificationPort     adaptador de notificaciones (inyectado por Spring)
     * @return instancia del servicio de asignación geoespacial
     */
    @Bean
    public AssignNearestDriverUseCase assignNearestDriverUseCase(
            OrderRepositoryPort orderRepository,
            DriverGeolocationPort driverGeolocationPort,
            NotificationPort notificationPort) {
        return new AssignNearestDriverService(
                orderRepository, driverGeolocationPort, notificationPort);
    }

    /**
     * Registra el caso de uso del cliente: creación y consulta de pedidos.
     *
     * @param orderRepository  adaptador de persistencia JPA/PostGIS
     * @param messagingTemplate plantilla para difusión WebSocket STOMP
     * @return instancia de ClientOrderService
     */
    @Bean
    public com.delivery.domain.port.input.ClientOrderUseCase clientOrderUseCase(
            OrderRepositoryPort orderRepository,
            org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate) {
        return new com.delivery.application.service.ClientOrderService(
                orderRepository, messagingTemplate);
    }

    /**
     * Registra el caso de uso del comercio: aceptar pedido e iniciar preparación.
     */
    @Bean
    public com.delivery.domain.port.input.AcceptOrderUseCase acceptOrderUseCase(
            OrderRepositoryPort orderRepository,
            DriverGeolocationPort driverGeolocationPort,
            AssignNearestDriverUseCase assignNearestDriverUseCase,
            org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate,
            NotificationPort notificationPort) {
        return new com.delivery.application.service.AcceptOrderService(
                orderRepository, driverGeolocationPort, assignNearestDriverUseCase, messagingTemplate, notificationPort);
    }

    /**
     * Registra el caso de uso del comercio: alternar disponibilidad de productos.
     */
    @Bean
    public com.delivery.domain.port.input.ToggleProductStockUseCase toggleProductStockUseCase(
            com.delivery.domain.port.output.ProductRepositoryPort productRepository) {
        return new com.delivery.application.service.ToggleProductStockService(productRepository);
    }

    /**
     * Registra el caso de uso del repartidor: aceptar viajes y validar PIN con liberación de saldo.
     */
    @Bean
    public com.delivery.domain.port.input.DriverDeliveryUseCase driverDeliveryUseCase(
            OrderRepositoryPort orderRepository,
            com.delivery.domain.port.output.DriverWalletPort driverWalletPort,
            NotificationPort notificationPort,
            org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate) {
        return new com.delivery.application.service.DriverDeliveryService(
                orderRepository, driverWalletPort, notificationPort, messagingTemplate);
    }
}
