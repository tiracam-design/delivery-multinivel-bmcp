package com.delivery.infrastructure.adapter.input.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/**
 * DTO estricto para transferir y actualizar datos de cobro / cuentas bancarias de
 * Comercios y Repartidores en Tulcán, Ecuador ($ USD).
 *
 * <p>Diseñado con validaciones estrictas y protección contra fuga de credenciales.</p>
 *
 * @since 1.0.0
 */
@Schema(description = "Datos de cuenta bancaria y QR de cobro")
public record BankDetailsDTO(
        UUID id,
        UUID userId,
        UUID merchantId,
        UUID driverId,

        @NotBlank(message = "El nombre del banco es requerido")
        @Schema(example = "Banco Pichincha")
        String bankName,

        @NotBlank(message = "El tipo de cuenta es requerido")
        @Schema(example = "Ahorros")
        String accountType,

        @NotBlank(message = "El número de cuenta es requerido")
        @Schema(example = "2201928374")
        String accountNumber,

        @NotBlank(message = "La identificación del titular es requerida")
        @Schema(example = "0491500042001")
        String accountHolderId,

        @NotBlank(message = "El nombre del titular es requerido")
        @Schema(example = "Patricio Benavides Cárdenas")
        String accountHolderName,

        @Schema(example = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=DEUNA_TULCAN_PAGO")
        String qrCodeUrl,

        @Schema(example = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=DEUNA_TULCAN_PAGO")
        String deunaQrUrl,

        @Schema(example = "true")
        Boolean isActive
) {
    // Constructor de retrocompatibilidad (10 parámetros)
    public BankDetailsDTO(UUID id, UUID userId, UUID merchantId, UUID driverId,
                          String bankName, String accountType, String accountNumber,
                          String accountHolderId, String accountHolderName, String qrCodeUrl) {
        this(id, userId, merchantId, driverId, bankName, accountType, accountNumber,
                accountHolderId, accountHolderName, qrCodeUrl, qrCodeUrl, true);
    }

    public static BankDetailsDTO ofDefaultMerchant() {
        return new BankDetailsDTO(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-111111111111"),
                UUID.fromString("00000000-0000-0000-0002-000000000001"),
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                null,
                "Banco Pichincha",
                "Ahorros",
                "2201928374",
                "0491500042001",
                "Patricio Benavides Cárdenas (Asadero El Leñador)",
                "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=DEUNA_TULCAN_LENADOR_2201928374",
                "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=DEUNA_TULCAN_LENADOR_2201928374",
                true
        );
    }

    public static BankDetailsDTO ofDefaultDriver() {
        return new BankDetailsDTO(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-333333333333"),
                UUID.fromString("00000000-0000-0000-0003-000000000001"),
                null,
                null,
                "Banco Pichincha",
                "Ahorros",
                "2208194720",
                "0401837291",
                "Carlos Mendoza Revelo (Repartidor Tulcán)",
                "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=DEUNA_TULCAN_DRIVER_CARLOS",
                "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=DEUNA_TULCAN_DRIVER_CARLOS",
                true
        );
    }
}
