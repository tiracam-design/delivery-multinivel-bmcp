package com.delivery.infrastructure.adapter.output.persistence.entity;

import com.delivery.infrastructure.adapter.output.persistence.converter.SensitiveDataCryptoConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad JPA que mapea la tabla {@code bank_accounts} en el esquema privado {@code bmcp_app}.
 * Almacena los datos bancarios y QR de cobro de Comercios y Repartidores en Tulcán ($ USD).
 *
 * <p>Aplica cifrado sutil a nivel de columna mediante {@link SensitiveDataCryptoConverter}
 * para salvaguardar el número de cuenta y la identificación del titular ante inspecciones directas.</p>
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "bank_accounts", schema = "bmcp_app")
public class BankAccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private UserEntity user;

    @Column(name = "merchant_id")
    private UUID merchantId;

    @Column(name = "driver_id")
    private UUID driverId;

    @Column(name = "bank_name", nullable = false, length = 100)
    private String bankName;

    @Column(name = "account_type", nullable = false, length = 50)
    private String accountType;

    @Convert(converter = SensitiveDataCryptoConverter.class)
    @Column(name = "account_number", nullable = false, length = 255)
    private String accountNumber;

    @Convert(converter = SensitiveDataCryptoConverter.class)
    @Column(name = "account_holder_id", nullable = false, length = 255)
    private String accountHolderId;

    @Column(name = "account_holder_name", nullable = false, length = 150)
    private String accountHolderName;

    @Column(name = "deuna_qr_url", columnDefinition = "TEXT")
    private String deunaQrUrl;

    @Column(name = "qr_code_url", columnDefinition = "TEXT")
    private String qrCodeUrl;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) this.createdAt = now;
        if (this.updatedAt == null) this.updatedAt = now;
        syncQrCodes();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
        syncQrCodes();
    }

    private void syncQrCodes() {
        if ((this.qrCodeUrl == null || this.qrCodeUrl.isBlank()) && this.deunaQrUrl != null) {
            this.qrCodeUrl = this.deunaQrUrl;
        } else if ((this.deunaQrUrl == null || this.deunaQrUrl.isBlank()) && this.qrCodeUrl != null) {
            this.deunaQrUrl = this.qrCodeUrl;
        }
    }

    protected BankAccountEntity() {}

    public BankAccountEntity(UUID userId, UUID merchantId, UUID driverId,
                             String bankName, String accountType, String accountNumber,
                             String accountHolderId, String accountHolderName, String qrCodeUrl) {
        this(userId, merchantId, driverId, bankName, accountType, accountNumber, accountHolderId, accountHolderName, qrCodeUrl, qrCodeUrl, true);
    }

    public BankAccountEntity(UUID userId, UUID merchantId, UUID driverId,
                             String bankName, String accountType, String accountNumber,
                             String accountHolderId, String accountHolderName,
                             String qrCodeUrl, String deunaQrUrl, boolean isActive) {
        this.userId = userId;
        this.merchantId = merchantId;
        this.driverId = driverId;
        this.bankName = bankName;
        this.accountType = accountType;
        this.accountNumber = accountNumber;
        this.accountHolderId = accountHolderId;
        this.accountHolderName = accountHolderName;
        this.qrCodeUrl = qrCodeUrl;
        this.deunaQrUrl = deunaQrUrl != null ? deunaQrUrl : qrCodeUrl;
        this.isActive = isActive;
    }

    // ── Métodos de Seguridad y Enmascaramiento ───────────────────────

    /**
     * Retorna una representación enmascarada del número de cuenta (ej. "******8374").
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getMaskedAccountNumber() {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return "****";
        }
        int visible = 4;
        String lastChars = accountNumber.substring(accountNumber.length() - visible);
        return "*".repeat(Math.max(0, accountNumber.length() - visible)) + lastChars;
    }

    /**
     * Retorna una representación enmascarada de la cédula / RUC (ej. "******42001").
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getMaskedAccountHolderId() {
        if (accountHolderId == null || accountHolderId.length() <= 4) {
            return "*****";
        }
        int visible = 4;
        String lastChars = accountHolderId.substring(accountHolderId.length() - visible);
        return "*".repeat(Math.max(0, accountHolderId.length() - visible)) + lastChars;
    }

    // ── Getters / Setters ───────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }

    public UUID getMerchantId() { return merchantId; }
    public void setMerchantId(UUID merchantId) { this.merchantId = merchantId; }

    public UUID getDriverId() { return driverId; }
    public void setDriverId(UUID driverId) { this.driverId = driverId; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getAccountHolderId() { return accountHolderId; }
    public void setAccountHolderId(String accountHolderId) { this.accountHolderId = accountHolderId; }

    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }

    public String getDeunaQrUrl() { return deunaQrUrl != null ? deunaQrUrl : qrCodeUrl; }
    public void setDeunaQrUrl(String deunaQrUrl) {
        this.deunaQrUrl = deunaQrUrl;
        if (this.qrCodeUrl == null || this.qrCodeUrl.isBlank()) {
            this.qrCodeUrl = deunaQrUrl;
        }
    }

    public String getQrCodeUrl() { return qrCodeUrl != null ? qrCodeUrl : deunaQrUrl; }
    public void setQrCodeUrl(String qrCodeUrl) {
        this.qrCodeUrl = qrCodeUrl;
        if (this.deunaQrUrl == null || this.deunaQrUrl.isBlank()) {
            this.deunaQrUrl = qrCodeUrl;
        }
    }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
