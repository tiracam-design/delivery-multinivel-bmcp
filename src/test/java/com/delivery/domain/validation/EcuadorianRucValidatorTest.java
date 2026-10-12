package com.delivery.domain.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Validaciones del Estándar Ecuatoriano de RUC (13 dígitos) y Carchi (04)")
class EcuadorianRucValidatorTest {

    @Test
    @DisplayName("Valida RUC ecuatoriano de persona natural en Carchi (13 dígitos terminados en 001)")
    void validNaturalPersonRuc_Carchi() {
        // Cédula válida de Carchi: 0401827365 -> RUC: 0401827365001
        // Verificación de cédula: 0*2 + 4*1 + 0*2 + 1*1 + 8*2(16->7) + 2*1 + 7*2(14->5) + 3*1 + 6*2(12->3)
        // Suma: 0 + 4 + 0 + 1 + 7 + 2 + 5 + 3 + 3 = 25. Verificador = 10 - (25%10) = 5.
        String rucCarchi = "0401827365001";
        assertTrue(EcuadorianRucValidator.isValid(rucCarchi));
        assertTrue(EcuadorianRucValidator.isValidCarchiRuc(rucCarchi));
        assertTrue(EcuadorianRucValidator.isCarchi(rucCarchi));
    }

    @Test
    @DisplayName("Valida RUC ecuatoriano de sociedad privada en Carchi (tercer dígito = 9)")
    void validPrivateCompanyRuc_Carchi() {
        // RUC Carchi sociedad: 0491500049001
        // Coeficientes [4, 3, 2, 7, 6, 5, 4, 3, 2]
        // 0*4 + 4*3(12) + 9*2(18) + 1*7(7) + 5*6(30) + 0*5(0) + 0*4(0) + 0*3(0) + 4*2(8)
        // Suma = 12 + 18 + 7 + 30 + 0 + 0 + 0 + 8 = 75
        // 75 % 11 = 9. 11 - 9 = 2. Verificador = 2.
        // RUC: 0491500042001
        String rucSociedad = "0491500042001";
        assertTrue(EcuadorianRucValidator.isValid(rucSociedad));
        assertTrue(EcuadorianRucValidator.isValidCarchiRuc(rucSociedad));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "20601234567",       // 11 dígitos (formato de Perú, no Ecuador)
            "040182736",         // 9 dígitos (incompleto)
            "040182736500",      // 12 dígitos (falta un dígito)
            "04018273650011",    // 14 dígitos (demasiados)
            "ABC01827365001",    // Letras
            "0401827365000",     // Establecimiento 000 inválido
            "2501827365001"      // Provincia 25 no existe en Ecuador (solo 01-24)
    })
    @DisplayName("Rechaza RUCs con longitud diferente a 13 dígitos o provincia inválida")
    void invalidRucs(String ruc) {
        assertFalse(EcuadorianRucValidator.isValid(ruc));
    }
}
