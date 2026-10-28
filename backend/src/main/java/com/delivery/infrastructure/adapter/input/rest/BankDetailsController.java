package com.delivery.infrastructure.adapter.input.rest;

import com.delivery.infrastructure.adapter.input.rest.dto.BankDetailsDTO;
import com.delivery.infrastructure.adapter.output.persistence.entity.BankAccountEntity;
import com.delivery.infrastructure.adapter.output.persistence.entity.UserEntity;
import com.delivery.infrastructure.adapter.output.persistence.repository.BankAccountJpaRepository;
import com.delivery.infrastructure.adapter.output.persistence.repository.UserJpaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

/**
 * Controlador REST para consultar y actualizar la información de cobro y
 * cuentas bancarias de los Comercios (ROLE_MERCHANT) y Repartidores (ROLE_DRIVER) en Tulcán ($ USD).
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping
@Tag(name = "Ajustes de Perfil - Datos Bancarios", description = "Gestión de información de cobro y QR de Comercios y Repartidores en Tulcán")
public class BankDetailsController {

    private final BankAccountJpaRepository bankAccountRepository;
    private final UserJpaRepository userRepository;

    public BankDetailsController(BankAccountJpaRepository bankAccountRepository,
                                 UserJpaRepository userRepository) {
        this.bankAccountRepository = bankAccountRepository;
        this.userRepository = userRepository;
    }

    // ── 1. Obtener datos bancarios del Comercio autenticado ──────────
    @GetMapping({"/v1/merchant/bank-details", "/api/v1/merchant/bank-details"})
    @PreAuthorize("hasAnyRole('MERCHANT', 'ADMIN')")
    @Operation(summary = "Consultar datos bancarios del comercio actual")
    public ResponseEntity<BankDetailsDTO> getMerchantBankDetails(Authentication auth) {
        UUID userId = resolveUserId(auth);

        Optional<BankAccountEntity> accountOpt = Optional.empty();
        if (userId != null) {
            accountOpt = bankAccountRepository.findFirstByUserIdOrderByUpdatedAtDesc(userId);
        }

        // Fallback a Asadero El Leñador de Tulcán si aún no ha configurado uno personalizado
        if (accountOpt.isEmpty()) {
            UUID defaultMerchantId = UUID.fromString("11111111-1111-1111-1111-111111111111");
            accountOpt = bankAccountRepository.findByMerchantId(defaultMerchantId);
        }

        BankDetailsDTO dto = accountOpt.map(this::toDTO).orElseGet(BankDetailsDTO::ofDefaultMerchant);
        return ResponseEntity.ok(dto);
    }

    // ── 2. Actualizar datos bancarios del Comercio ──────────────────
    @PutMapping({"/v1/merchant/bank-details", "/api/v1/merchant/bank-details"})
    @PreAuthorize("hasAnyRole('MERCHANT', 'ADMIN')")
    @Transactional
    @Operation(summary = "Actualizar datos bancarios del comercio")
    public ResponseEntity<BankDetailsDTO> updateMerchantBankDetails(
            @Valid @RequestBody BankDetailsDTO dto,
            Authentication auth) {
        UUID userId = resolveUserId(auth);
        UUID merchantId = dto.merchantId() != null
                ? dto.merchantId()
                : UUID.fromString("11111111-1111-1111-1111-111111111111");

        BankAccountEntity entity = bankAccountRepository.findByMerchantId(merchantId)
                .or(() -> userId != null ? bankAccountRepository.findFirstByUserIdOrderByUpdatedAtDesc(userId) : Optional.empty())
                .orElseGet(() -> new BankAccountEntity(userId, merchantId, null,
                        dto.bankName(), dto.accountType(), dto.accountNumber(),
                        dto.accountHolderId(), dto.accountHolderName(), dto.qrCodeUrl()));

        entity.setBankName(dto.bankName().trim());
        entity.setAccountType(dto.accountType().trim());
        entity.setAccountNumber(dto.accountNumber().trim());
        entity.setAccountHolderId(dto.accountHolderId().trim());
        entity.setAccountHolderName(dto.accountHolderName().trim());
        entity.setQrCodeUrl(dto.qrCodeUrl() != null && !dto.qrCodeUrl().isBlank() ? dto.qrCodeUrl().trim() : null);

        if (userId != null && entity.getUserId() == null) {
            entity.setUserId(userId);
        }
        if (entity.getMerchantId() == null) {
            entity.setMerchantId(merchantId);
        }

        if (dto.deunaQrUrl() != null && !dto.deunaQrUrl().isBlank()) {
            entity.setDeunaQrUrl(dto.deunaQrUrl().trim());
        }
        if (dto.isActive() != null) {
            entity.setActive(dto.isActive());
        }

        BankAccountEntity saved = bankAccountRepository.save(entity);
        return ResponseEntity.ok(toDTO(saved));
    }

    // ── 3. Obtener datos bancarios del Repartidor autenticado ─────────
    @GetMapping({"/v1/driver/bank-details", "/api/v1/driver/bank-details"})
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    @Operation(summary = "Consultar datos bancarios del repartidor actual")
    public ResponseEntity<BankDetailsDTO> getDriverBankDetails(Authentication auth) {
        UUID userId = resolveUserId(auth);
        Optional<BankAccountEntity> accountOpt = Optional.empty();
        if (userId != null) {
            accountOpt = bankAccountRepository.findFirstByUserIdOrderByUpdatedAtDesc(userId);
        }
        if (accountOpt.isEmpty()) {
            UUID defaultDriverUserId = UUID.fromString("00000000-0000-0000-0003-000000000001");
            accountOpt = bankAccountRepository.findByUserId(defaultDriverUserId);
        }

        BankDetailsDTO dto = accountOpt.map(this::toDTO).orElseGet(BankDetailsDTO::ofDefaultDriver);
        return ResponseEntity.ok(dto);
    }

    // ── 4. Actualizar datos bancarios del Repartidor ─────────────────
    @PutMapping({"/v1/driver/bank-details", "/api/v1/driver/bank-details"})
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    @Transactional
    @Operation(summary = "Actualizar datos bancarios del repartidor")
    public ResponseEntity<BankDetailsDTO> updateDriverBankDetails(
            @Valid @RequestBody BankDetailsDTO dto,
            Authentication auth) {
        UUID userId = resolveUserId(auth);
        if (userId == null) {
            userId = UUID.fromString("00000000-0000-0000-0003-000000000001");
        }

        final UUID finalUserId = userId;
        BankAccountEntity entity = bankAccountRepository.findByUserId(finalUserId)
                .orElseGet(() -> new BankAccountEntity(finalUserId, null, null,
                        dto.bankName(), dto.accountType(), dto.accountNumber(),
                        dto.accountHolderId(), dto.accountHolderName(), dto.qrCodeUrl()));

        entity.setBankName(dto.bankName().trim());
        entity.setAccountType(dto.accountType().trim());
        entity.setAccountNumber(dto.accountNumber().trim());
        entity.setAccountHolderId(dto.accountHolderId().trim());
        entity.setAccountHolderName(dto.accountHolderName().trim());
        entity.setQrCodeUrl(dto.qrCodeUrl() != null && !dto.qrCodeUrl().isBlank() ? dto.qrCodeUrl().trim() : null);
        if (dto.deunaQrUrl() != null && !dto.deunaQrUrl().isBlank()) {
            entity.setDeunaQrUrl(dto.deunaQrUrl().trim());
        }
        if (dto.isActive() != null) {
            entity.setActive(dto.isActive());
        }

        BankAccountEntity saved = bankAccountRepository.save(entity);
        return ResponseEntity.ok(toDTO(saved));
    }

    // ── 5. Consulta pública / cliente de datos bancarios para Checkout ─
    @GetMapping({"/v1/client/merchants/{merchantId}/bank-details", "/api/v1/client/merchants/{merchantId}/bank-details"})
    @Operation(summary = "Consultar datos bancarios de un comercio para pagos en checkout")
    public ResponseEntity<BankDetailsDTO> getMerchantBankDetailsForClient(@PathVariable UUID merchantId) {
        Optional<BankAccountEntity> accountOpt = bankAccountRepository.findByMerchantId(merchantId);
        BankDetailsDTO dto = accountOpt.map(this::toDTO).orElseGet(BankDetailsDTO::ofDefaultMerchant);
        return ResponseEntity.ok(dto);
    }

    private BankDetailsDTO toDTO(BankAccountEntity entity) {
        return new BankDetailsDTO(
                entity.getId(),
                entity.getUserId(),
                entity.getMerchantId(),
                entity.getDriverId(),
                entity.getBankName(),
                entity.getAccountType(),
                entity.getAccountNumber(),
                entity.getAccountHolderId(),
                entity.getAccountHolderName(),
                entity.getQrCodeUrl(),
                entity.getDeunaQrUrl(),
                entity.isActive()
        );
    }

    private UUID resolveUserId(Authentication auth) {
        if (auth == null || auth.getName() == null) return null;
        String email = auth.getName().trim().toLowerCase();
        return userRepository.findByEmail(email).map(UserEntity::getId).orElse(null);
    }
}
