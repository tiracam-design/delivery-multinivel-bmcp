package com.delivery.domain.validation;

import java.util.regex.Pattern;

/**
 * Validador oficial del Registro Único de Contribuyentes (RUC) del Servicio de Rentas Internas (SRI)
 * de la República del Ecuador.
 *
 * <p>Estructura y reglas técnicas:</p>
 * <ul>
 *   <li>Longitud estricta: 13 dígitos numéricos.</li>
 *   <li>Código de provincia (primeros 2 dígitos): entre 01 y 24 (o 30 para residentes especiales sin cédula).
 *       El código para la provincia del Carchi (Tulcán) es {@code 04}.</li>
 *   <li>Tercer dígito:
 *     <ul>
 *       <li><strong>Persona Natural (dígito 0 a 5):</strong> Los primeros 10 dígitos corresponden a la cédula
 *           de identidad (validada mediante el algoritmo de Luhn / Módulo 10 del Registro Civil).
 *           Los últimos 3 dígitos corresponden al código de establecimiento (típicamente {@code 001}).</li>
 *       <li><strong>Sociedad Privada / Extranjeros sin cédula (dígito 9):</strong> Los dígitos del 4 al 9 son secuenciales,
 *           el dígito 10 es el verificador calculado mediante algoritmo Módulo 11 con coeficientes
 *           {@code [4, 3, 2, 7, 6, 5, 4, 3, 2]}. Los últimos 3 dígitos corresponden al establecimiento ({@code 001}).</li>
 *       <li><strong>Institución Pública (dígito 6):</strong> Algoritmo Módulo 11 con coeficientes
 *           {@code [3, 2, 7, 6, 5, 4, 3, 2]} en el dígito 9, y los últimos 4 dígitos típicamente {@code 0001}.</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * @since 1.0.0
 */
public final class EcuadorianRucValidator {

    private static final Pattern NUMERIC_13_DIGITS = Pattern.compile("^\\d{13}$");
    public static final String CARCHI_PROVINCE_CODE = "04";

    private EcuadorianRucValidator() {}

    /**
     * Valida si una cadena cumple estrictamente con el formato y los algoritmos del RUC de Ecuador.
     *
     * @param ruc cadena con el RUC de 13 dígitos
     * @return true si es un RUC ecuatoriano válido, false en caso contrario
     */
    public static boolean isValid(String ruc) {
        if (ruc == null || !NUMERIC_13_DIGITS.matcher(ruc).matches()) {
            return false;
        }

        int province = Integer.parseInt(ruc.substring(0, 2));
        if ((province < 1 || province > 24) && province != 30) {
            return false;
        }

        int thirdDigit = Character.getNumericValue(ruc.charAt(2));

        if (thirdDigit >= 0 && thirdDigit <= 5) {
            // Persona natural: primeros 10 dígitos son cédula ecuatoriana válida, últimos 3 >= 001
            String establishment = ruc.substring(10, 13);
            if (establishment.equals("000")) {
                return false;
            }
            return isValidEcuadorianCedula(ruc.substring(0, 10));
        } else if (thirdDigit == 9) {
            // Sociedad privada: algoritmo módulo 11
            String establishment = ruc.substring(10, 13);
            if (establishment.equals("000")) {
                return false;
            }
            return isValidPrivateCompanyRuc(ruc);
        } else if (thirdDigit == 6) {
            // Entidad pública: algoritmo módulo 11
            String establishment = ruc.substring(9, 13);
            if (establishment.equals("0000")) {
                return false;
            }
            return isValidPublicEntityRuc(ruc);
        }

        return false;
    }

    /**
     * Valida si el RUC es ecuatoriano válido y además pertenece a la provincia del Carchi (código 04).
     */
    public static boolean isValidCarchiRuc(String ruc) {
        return isValid(ruc) && isCarchi(ruc);
    }

    /**
     * Comprueba si los 2 primeros dígitos corresponden al código provincial del Carchi (04).
     */
    public static boolean isCarchi(String ruc) {
        return ruc != null && ruc.length() >= 2 && ruc.startsWith(CARCHI_PROVINCE_CODE);
    }

    /**
     * Algoritmo de validación de Cédula de Identidad de Ecuador (Módulo 10).
     */
    public static boolean isValidEcuadorianCedula(String cedula) {
        if (cedula == null || cedula.length() != 10) {
            return false;
        }

        int[] coefficients = {2, 1, 2, 1, 2, 1, 2, 1, 2};
        int sum = 0;

        for (int i = 0; i < 9; i++) {
            int val = Character.getNumericValue(cedula.charAt(i)) * coefficients[i];
            sum += (val >= 10) ? (val - 9) : val;
        }

        int checkDigit = Character.getNumericValue(cedula.charAt(9));
        int calculatedDigit = (sum % 10 == 0) ? 0 : 10 - (sum % 10);

        return checkDigit == calculatedDigit;
    }

    /**
     * Algoritmo Módulo 11 para Sociedades Privadas (tercer dígito = 9).
     */
    private static boolean isValidPrivateCompanyRuc(String ruc) {
        int[] coefficients = {4, 3, 2, 7, 6, 5, 4, 3, 2};
        int sum = 0;

        for (int i = 0; i < 9; i++) {
            sum += Character.getNumericValue(ruc.charAt(i)) * coefficients[i];
        }

        int checkDigit = Character.getNumericValue(ruc.charAt(9));
        int remainder = sum % 11;
        int calculated = (remainder == 0) ? 0 : 11 - remainder;

        return checkDigit == calculated;
    }

    /**
     * Algoritmo Módulo 11 para Entidades Públicas (tercer dígito = 6).
     */
    private static boolean isValidPublicEntityRuc(String ruc) {
        int[] coefficients = {3, 2, 7, 6, 5, 4, 3, 2};
        int sum = 0;

        for (int i = 0; i < 8; i++) {
            sum += Character.getNumericValue(ruc.charAt(i)) * coefficients[i];
        }

        int checkDigit = Character.getNumericValue(ruc.charAt(8));
        int remainder = sum % 11;
        int calculated = (remainder == 0) ? 0 : 11 - remainder;

        return checkDigit == calculated;
    }
}
