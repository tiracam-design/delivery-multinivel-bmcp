package com.delivery.application.service;

import com.delivery.domain.enums.NotificationType;
import com.delivery.domain.exception.OrderNotFoundException;
import com.delivery.domain.model.Driver;
import com.delivery.domain.model.Order;
import com.delivery.domain.port.input.AssignNearestDriverUseCase;
import com.delivery.domain.port.input.command.AssignNearestDriverCommand;
import com.delivery.domain.port.output.DriverGeolocationPort;
import com.delivery.domain.port.output.NotificationPort;
import com.delivery.domain.port.output.OrderRepositoryPort;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Implementación del caso de uso: Asignar Repartidor Más Cercano.
 *
 * <p>Orquesta la búsqueda geoespacial (PostGIS) de repartidores
 * disponibles dentro del radio indicado y aplica el algoritmo de
 * prioridad multinivel para seleccionar al mejor candidato.</p>
 *
 * <h3>Algoritmo de selección (RULE-003):</h3>
 * <ol>
 *   <li>Recuperar el pedido y validar estado + geolocalización</li>
 *   <li>Buscar repartidores activos dentro del radio vía
 *       {@link DriverGeolocationPort#findAvailableDriversWithinRadius}</li>
 *   <li>Aplicar prioridad multinivel:
 *       <ul>
 *         <li>Nivel superior primero (MASTER &gt; SENIOR &gt; JUNIOR)</li>
 *         <li>A igual nivel, menor distancia gana</li>
 *       </ul>
 *   </li>
 *   <li>Asignar el mejor candidato al pedido</li>
 *   <li>Persistir la asignación</li>
 *   <li>Notificar al cliente que un repartidor fue asignado</li>
 * </ol>
 *
 * @see AssignNearestDriverUseCase
 * @see Driver#hasHigherPriorityThan(Driver)
 * @since 1.0.0
 */
public class AssignNearestDriverService implements AssignNearestDriverUseCase {

    private static final Logger LOG = Logger.getLogger(
            AssignNearestDriverService.class.getName());

    private final OrderRepositoryPort orderRepository;
    private final DriverGeolocationPort driverGeolocationPort;
    private final NotificationPort notificationPort;

    public AssignNearestDriverService(OrderRepositoryPort orderRepository,
                                      DriverGeolocationPort driverGeolocationPort,
                                      NotificationPort notificationPort) {
        this.orderRepository = Objects.requireNonNull(orderRepository,
                "orderRepository no puede ser null");
        this.driverGeolocationPort = Objects.requireNonNull(driverGeolocationPort,
                "driverGeolocationPort no puede ser null");
        this.notificationPort = Objects.requireNonNull(notificationPort,
                "notificationPort no puede ser null");
    }

    @Override
    public Optional<Driver> execute(AssignNearestDriverCommand command) {
        Objects.requireNonNull(command, "command no puede ser null");

        UUID orderId = command.getOrderId();
        double radiusKm = command.getRadiusKm();

        // ── Paso 1: Recuperar y validar la orden ────────────────────
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    LOG.warning(() -> String.format(
                            "[ASSIGN] Pedido no encontrado. orderId=%s", orderId));
                    return new OrderNotFoundException(orderId);
                });

        if (order.getStatus().isTerminal()) {
            throw new IllegalStateException(String.format(
                    "No se puede asignar repartidor a pedido en estado terminal: %s",
                    order.getStatus()));
        }

        if (order.getPickupLocation() == null) {
            throw new IllegalStateException(String.format(
                    "El pedido %s no tiene ubicación de recogida (pickup_location)",
                    orderId));
        }

        // ── Paso 2: Buscar repartidores dentro del radio ────────────
        List<Driver> candidates = driverGeolocationPort
                .findAvailableDriversWithinRadius(
                        order.getPickupLocation(), radiusKm);

        LOG.info(() -> String.format(
                "[ASSIGN] Búsqueda de repartidores. orderId=%s, radio=%.1f km, "
                        + "candidatos=%d",
                orderId, radiusKm, candidates.size()));

        if (candidates.isEmpty()) {
            LOG.info(() -> String.format(
                    "[ASSIGN] No se encontraron repartidores dentro de %.1f km "
                            + "para pedido %s",
                    radiusKm, orderId));
            return Optional.empty();
        }

        // ── Paso 3: Seleccionar mejor candidato (prioridad multinivel)
        Driver bestCandidate = candidates.stream()
                .reduce((a, b) -> a.hasHigherPriorityThan(b) ? a : b)
                .orElseThrow(); // nunca vacío, ya validamos isEmpty()

        LOG.info(() -> String.format(
                "[ASSIGN] Mejor candidato seleccionado. orderId=%s, "
                        + "driverId=%s, driver='%s', nivel=%s, distancia=%.2f km",
                orderId, bestCandidate.getId(), bestCandidate.getFullName(),
                bestCandidate.getLevel(),
                bestCandidate.getDistanceKm()));

        // ── Paso 4: Asignar al pedido y persistir ───────────────────
        order.assignDriver(bestCandidate.getId());
        orderRepository.save(order);

        // ── Paso 5: Notificar al cliente ────────────────────────────
        notificationPort.notifyCustomer(
                order.getCustomerId(),
                NotificationType.DRIVER_ASSIGNED,
                buildDriverAssignedPayload(order, bestCandidate)
        );

        // Notificar al repartidor asignado
        notificationPort.notifyDriver(
                bestCandidate.getId(),
                NotificationType.DRIVER_ASSIGNED,
                buildDriverAssignedPayload(order, bestCandidate)
        );

        return Optional.of(bestCandidate);
    }

    /**
     * Construye el payload de notificación para la asignación de repartidor.
     */
    private Map<String, Object> buildDriverAssignedPayload(Order order,
                                                            Driver driver) {
        return Map.of(
                "orderId", order.getId(),
                "driverName", driver.getFullName(),
                "driverPhone", driver.getPhone() != null ? driver.getPhone() : "",
                "driverLevel", driver.getLevel().name(),
                "estimatedDistanceKm", driver.getDistanceKm() != null
                        ? driver.getDistanceKm() : 0.0,
                "pickupAddress", order.getPickupAddress()
        );
    }
}
