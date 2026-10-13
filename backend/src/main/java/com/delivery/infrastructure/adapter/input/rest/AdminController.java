package com.delivery.infrastructure.adapter.input.rest;

import com.delivery.infrastructure.adapter.output.persistence.entity.DriverEntity;
import com.delivery.infrastructure.adapter.output.persistence.entity.MerchantEntity;
import com.delivery.infrastructure.adapter.output.persistence.repository.DriverJpaRepository;
import com.delivery.infrastructure.adapter.output.persistence.repository.MerchantJpaRepository;
import com.delivery.infrastructure.adapter.output.persistence.repository.OrderJpaRepository;
import com.delivery.infrastructure.adapter.output.persistence.repository.UserJpaRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

/**
 * Adaptador de Entrada REST — Módulo de Administración (BMCP Tulcán).
 *
 * <p>Expone los endpoints exclusivos para la supervisión y auditoría del sistema:
 * <ul>
 *   <li>Auditoría y aprobación/rechazo de comercios (RUC ecuatoriano y documentación).</li>
 *   <li>Auditoría y aprobación/rechazo de repartidores (Licencia ANT y SOAT).</li>
 *   <li>Métricas y KPIs globales del sistema en tiempo real.</li>
 * </ul>
 * </p>
 *
 * <h3>Ruta base:</h3>
 * {@code /v1/admin} y {@code /admin}
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping({"/v1/admin", "/admin"})
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Módulo Administrador", description = "Endpoints de control, auditoría legal y métricas globales")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final MerchantJpaRepository merchantJpaRepository;
    private final DriverJpaRepository driverJpaRepository;
    private final OrderJpaRepository orderJpaRepository;
    private final UserJpaRepository userJpaRepository;

    public AdminController(MerchantJpaRepository merchantJpaRepository,
                           DriverJpaRepository driverJpaRepository,
                           OrderJpaRepository orderJpaRepository,
                           UserJpaRepository userJpaRepository) {
        this.merchantJpaRepository = Objects.requireNonNull(merchantJpaRepository);
        this.driverJpaRepository = Objects.requireNonNull(driverJpaRepository);
        this.orderJpaRepository = Objects.requireNonNull(orderJpaRepository);
        this.userJpaRepository = Objects.requireNonNull(userJpaRepository);
    }

    // ── 1. MÉTRICAS Y KPIS GLOBALES ──────────────────────────────────────

    @GetMapping("/metrics")
    @Operation(summary = "Métricas y KPIs del sistema", description = "Retorna contadores de comercios, repartidores, pedidos y ventas para el panel de administración.")
    public ResponseEntity<?> getSystemMetrics() {
        long totalUsers = userJpaRepository.count();
        long totalMerchants = merchantJpaRepository.count();
        long pendingMerchants = merchantJpaRepository.findByActiveFalse().size();
        long totalDrivers = driverJpaRepository.count();
        long pendingDrivers = driverJpaRepository.findByActiveFalse().size();
        long totalOrders = orderJpaRepository.count();

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("totalUsers", totalUsers);
        metrics.put("totalMerchants", totalMerchants);
        metrics.put("pendingMerchantsCount", pendingMerchants);
        metrics.put("totalDrivers", totalDrivers);
        metrics.put("pendingDriversCount", pendingDrivers);
        metrics.put("totalOrders", totalOrders);
        metrics.put("currency", "USD");
        metrics.put("city", "Tulcán, Carchi");
        metrics.put("timestamp", Instant.now().toString());

        return ResponseEntity.ok(metrics);
    }

    // ── 2. AUDITORÍA DE COMERCIOS ────────────────────────────────────────

    @GetMapping("/merchants/pending")
    @Operation(summary = "Comercios pendientes de aprobación", description = "Obtiene la lista de comercios que esperan verificación de RUC.")
    public ResponseEntity<List<MerchantEntity>> getPendingMerchants() {
        return ResponseEntity.ok(merchantJpaRepository.findByActiveFalse());
    }

    @PostMapping("/merchants/{merchantId}/approve")
    @Transactional
    @Operation(summary = "Aprobar comercio", description = "Activa el comercio tras validar su RUC ecuatoriano y documentos legales.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comercio aprobado y habilitado"),
            @ApiResponse(responseCode = "404", description = "Comercio no encontrado")
    })
    public ResponseEntity<?> approveMerchant(
            @Parameter(description = "ID del comercio a aprobar")
            @PathVariable UUID merchantId) {
        Optional<MerchantEntity> opt = merchantJpaRepository.findById(merchantId);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        MerchantEntity merchant = opt.get();
        merchant.setActive(true);
        merchantJpaRepository.save(merchant);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("merchantId", merchant.getId());
        response.put("name", merchant.getName());
        response.put("active", true);
        response.put("message", "Comercio aprobado exitosamente y habilitado en Tulcán.");
        response.put("approvedAt", Instant.now().toString());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/merchants/{merchantId}/reject")
    @Transactional
    @Operation(summary = "Rechazar comercio", description = "Suspende o rechaza la solicitud de un comercio con motivo documentado.")
    public ResponseEntity<?> rejectMerchant(
            @PathVariable UUID merchantId,
            @RequestBody(required = false) Map<String, String> body) {
        Optional<MerchantEntity> opt = merchantJpaRepository.findById(merchantId);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String reason = (body != null && body.containsKey("reason")) ? body.get("reason") : "Documentación o RUC inconsistente";
        MerchantEntity merchant = opt.get();
        merchant.setActive(false);
        merchantJpaRepository.save(merchant);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("merchantId", merchant.getId());
        response.put("name", merchant.getName());
        response.put("active", false);
        response.put("reason", reason);
        response.put("message", "Solicitud de comercio rechazada.");
        response.put("rejectedAt", Instant.now().toString());

        return ResponseEntity.ok(response);
    }

    // ── 3. AUDITORÍA DE REPARTIDORES ─────────────────────────────────────

    @GetMapping("/drivers/pending")
    @Operation(summary = "Repartidores pendientes de aprobación", description = "Obtiene repartidores registrados que esperan validación de licencia ANT y SOAT.")
    public ResponseEntity<List<DriverEntity>> getPendingDrivers() {
        return ResponseEntity.ok(driverJpaRepository.findByActiveFalse());
    }

    @PostMapping("/drivers/{driverId}/approve")
    @Transactional
    @Operation(summary = "Aprobar repartidor", description = "Aprueba al repartidor para operar en la flota tras verificar licencia Tipo A/B y SOAT.")
    public ResponseEntity<?> approveDriver(@PathVariable UUID driverId) {
        Optional<DriverEntity> opt = driverJpaRepository.findById(driverId);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        DriverEntity driver = opt.get();
        driver.setActive(true);
        driverJpaRepository.save(driver);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("driverId", driver.getId());
        response.put("vehiclePlate", driver.getVehiclePlate());
        response.put("active", true);
        response.put("message", "Repartidor aprobado y activado para recibir viajes.");
        response.put("approvedAt", Instant.now().toString());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/drivers/{driverId}/reject")
    @Transactional
    @Operation(summary = "Rechazar repartidor", description = "Rechaza la solicitud del conductor con motivo de auditoría.")
    public ResponseEntity<?> rejectDriver(
            @PathVariable UUID driverId,
            @RequestBody(required = false) Map<String, String> body) {
        Optional<DriverEntity> opt = driverJpaRepository.findById(driverId);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String reason = (body != null && body.containsKey("reason")) ? body.get("reason") : "Licencia no válida o SOAT vencido";
        DriverEntity driver = opt.get();
        driver.setActive(false);
        driverJpaRepository.save(driver);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("driverId", driver.getId());
        response.put("active", false);
        response.put("reason", reason);
        response.put("message", "Solicitud de repartidor rechazada.");
        response.put("rejectedAt", Instant.now().toString());

        return ResponseEntity.ok(response);
    }
}
