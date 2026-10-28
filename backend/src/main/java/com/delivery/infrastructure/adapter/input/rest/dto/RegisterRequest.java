package com.delivery.infrastructure.adapter.input.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Solicitud de registro de usuario real en PostgreSQL")
public record RegisterRequest(
        @NotBlank(message = "El nombre completo es requerido")
        @Schema(example = "Valeria Narváez Guerrón")
        String fullName,

        @NotBlank(message = "El correo electrónico es requerido")
        @Email(message = "Formato de correo electrónico inválido")
        @Schema(example = "valeria.narvaez@tulcan.ec")
        String email,

        @NotBlank(message = "La contraseña es requerida")
        @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
        @Schema(example = "password123")
        String password,

        @Schema(example = "CLIENT", description = "Rol objetivo: CLIENT, DRIVER, MERCHANT, ADMIN")
        String role,

        @Schema(example = "+593 99 123 4567")
        String phone,

        @Schema(example = "Banco Pichincha")
        String bankName,

        @Schema(example = "Ahorros")
        String accountType,

        @Schema(example = "2201928374")
        String accountNumber,

        @Schema(example = "0401234567")
        String accountHolderId,

        @Schema(example = "Asadero El Leñador")
        String accountHolderName,

        @Schema(example = "https://example.com/qr.png")
        String qrCodeUrl
) {
    public RegisterRequest(String fullName, String email, String password, String role, String phone) {
        this(fullName, email, password, role, phone, null, null, null, null, null, null);
    }
}
