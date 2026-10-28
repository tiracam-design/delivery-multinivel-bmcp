package com.delivery.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Modelo de dominio que representa una cuenta de cobro / bancaria en Tulcán ($ USD).
 *
 * <p>POJO puro sin dependencias de frameworks, con soporte para
 * enmascaramiento de datos sensibles en proyecciones de lectura.</p>
 *
 * @since 1.0.0
 */
public class BankAccount {

    private final UUID id;
    private final UUID userId;
    private final UUID merchantId;
    private final UUID driverId;
    private final String bankName;
    private final String accountType;
    private final String accountNumber;
    private final String accountHolderId;
    private final String accountHolderName;
    private final String deunaQrUrl;
    private final boolean isActive;
    private final Instant createdAt;
    private final Instant updatedAt;

    public BankAccount(UUID id, UUID userId, UUID merchantId, UUID driverId,
                       String bankName, String accountType, String accountNumber,
                       String accountHolderId, String accountHolderName,
                       String deunaQrUrl, boolean isActive,
                       Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.merchantId = merchantId;
        this.driverId = driverId;
        this.bankName = Objects.requireNonNull(bankName, "bankName no puede ser null");
        this.accountType = Objects.requireNonNull(accountType, "accountType no puede ser null");
        this.accountNumber = Objects.requireNonNull(accountNumber, "accountNumber no puede ser null");
        this.accountHolderId = Objects.requireNonNull(accountHolderId, "accountHolderId no puede ser null");
        this.accountHolderName = Objects.requireNonNull(accountHolderName, "accountHolderName no puede ser null");
        this.deunaQrUrl = deunaQrUrl;
        this.isActive = isActive;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getMerchantId() { return merchantId; }
    public UUID getDriverId() { return driverId; }
    public String getBankName() { return bankName; }
    public String getAccountType() { return accountType; }
    public String getAccountNumber() { return accountNumber; }
    public String getAccountHolderId() { return accountHolderId; }
    public String getAccountHolderName() { return accountHolderName; }
    public String getDeunaQrUrl() { return deunaQrUrl; }
    public boolean isActive() { return isActive; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    /**
     * Enmascara el número de cuenta para exposición segura (ej. "******8374").
     */
    public String getMaskedAccountNumber() {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return "****";
        }
        int visible = 4;
        String lastChars = accountNumber.substring(accountNumber.length() - visible);
        return "*".repeat(Math.max(0, accountNumber.length() - visible)) + lastChars;
    }

    /**
     * Enmascara la cédula / RUC del titular para exposición segura (ej. "******42001").
     */
    public String getMaskedAccountHolderId() {
        if (accountHolderId == null || accountHolderId.length() <= 4) {
            return "*****";
        }
        int visible = 4;
        String lastChars = accountHolderId.substring(accountHolderId.length() - visible);
        return "*".repeat(Math.max(0, accountHolderId.length() - visible)) + lastChars;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BankAccount that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("BankAccount[id=%s, bank='%s', type='%s', account='%s', holder='%s', active=%s]",
                id, bankName, accountType, getMaskedAccountNumber(), accountHolderName, isActive);
    }
}
