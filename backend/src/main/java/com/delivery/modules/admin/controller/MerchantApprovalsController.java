package com.delivery.modules.admin.controller;

import com.delivery.modules.admin.dto.DriverApprovalResponseDTO;
import com.delivery.modules.admin.dto.MerchantApprovalResponseDTO;
import com.delivery.modules.admin.service.MerchantApprovalsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controlador Spring Boot para el Módulo ADMIN (Aprobaciones & RUC).
 *
 * <p>Equivalente en el servidor de las operaciones de auditoría legal y fiscal.</p>
 */
@RestController
@RequestMapping({"/v1/modules/admin/approvals", "/v1/admin/approvals", "/api/v1/admin/approvals", "/api/v1/modules/admin/approvals"})
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Módulo Admin - Aprobaciones", description = "Auditoría legal de comercios y repartidores de Tulcán")
public class MerchantApprovalsController {

    private final MerchantApprovalsService service;

    public MerchantApprovalsController(MerchantApprovalsService service) {
        this.service = service;
    }

    @GetMapping("/merchants")
    @Operation(summary = "Comercios pendientes de verificación")
    public ResponseEntity<List<MerchantApprovalResponseDTO>> getPendingMerchants() {
        return ResponseEntity.ok(service.getPendingMerchants());
    }

    @PostMapping("/merchants/{id}/approve")
    @Operation(summary = "Aprobar comercio por administración")
    public ResponseEntity<?> approveMerchant(@PathVariable UUID id) {
        return service.approveMerchant(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/merchants/{id}/reject")
    @Operation(summary = "Rechazar comercio por administración")
    public ResponseEntity<?> rejectMerchant(@PathVariable UUID id, @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.getOrDefault("reason", "Incumplimiento de requisitos") : "Incumplimiento de requisitos";
        return service.rejectMerchant(id, reason)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/drivers")
    @Operation(summary = "Repartidores pendientes de verificación ANT/SOAT")
    public ResponseEntity<List<DriverApprovalResponseDTO>> getPendingDrivers() {
        return ResponseEntity.ok(service.getPendingDrivers());
    }

    @PostMapping("/drivers/{id}/approve")
    @Operation(summary = "Aprobar repartidor")
    public ResponseEntity<?> approveDriver(@PathVariable UUID id) {
        return service.approveDriver(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/drivers/{id}/reject")
    @Operation(summary = "Rechazar repartidor")
    public ResponseEntity<?> rejectDriver(@PathVariable UUID id, @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.getOrDefault("reason", "Documentos no válidos") : "Documentos no válidos";
        return service.rejectDriver(id, reason)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
