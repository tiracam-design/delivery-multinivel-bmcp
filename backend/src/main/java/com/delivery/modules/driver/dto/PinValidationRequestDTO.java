package com.delivery.modules.driver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PinValidationRequestDTO(
        @NotBlank(message = "El PIN es requerido")
        @Pattern(regexp = "^\\d{4}$", message = "El PIN debe contener exactamente 4 dígitos")
        String pin
) {}
