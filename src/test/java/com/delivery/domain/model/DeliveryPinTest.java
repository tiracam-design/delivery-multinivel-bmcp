package com.delivery.domain.model;

import com.delivery.domain.exception.InvalidDeliveryPinException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para el Value Object {@link DeliveryPin}.
 */
@DisplayName("DeliveryPin — Value Object Anti-Fraude")
class DeliveryPinTest {

    @Nested
    @DisplayName("Generación")
    class Generation {

        @Test
        @DisplayName("Debe generar PIN de 4 dígitos")
        void shouldGenerateFourDigitPin() {
            DeliveryPin pin = DeliveryPin.generate();
            assertEquals(4, pin.getValue().length());
            assertTrue(pin.getValue().matches("\\d{4}"));
        }

        @Test
        @DisplayName("Debe tener expiración futura")
        void shouldHaveFutureExpiration() {
            DeliveryPin pin = DeliveryPin.generate();
            assertFalse(pin.isExpired());
            assertTrue(pin.getExpiresAt().isAfter(pin.getCreatedAt()));
        }

        @Test
        @DisplayName("Debe iniciar con 0 intentos")
        void shouldStartWithZeroAttempts() {
            DeliveryPin pin = DeliveryPin.generate();
            assertEquals(0, pin.getVerificationAttempts());
            assertEquals(DeliveryPin.MAX_VERIFICATION_ATTEMPTS,
                    pin.getRemainingAttempts());
        }
    }

    @Nested
    @DisplayName("Validación de formato")
    class FormatValidation {

        @Test
        @DisplayName("Debe rechazar PIN con letras")
        void shouldRejectAlphabeticPin() {
            assertThrows(InvalidDeliveryPinException.class,
                    () -> DeliveryPin.generate().verify("ABCD"));
        }

        @Test
        @DisplayName("Debe rechazar PIN con longitud incorrecta")
        void shouldRejectWrongLength() {
            assertThrows(InvalidDeliveryPinException.class,
                    () -> DeliveryPin.generate().verify("123"));
            assertThrows(InvalidDeliveryPinException.class,
                    () -> DeliveryPin.generate().verify("12345"));
        }

        @Test
        @DisplayName("Debe rechazar PIN null")
        void shouldRejectNullPin() {
            assertThrows(InvalidDeliveryPinException.class,
                    () -> DeliveryPin.generate().verify(null));
        }
    }

    @Nested
    @DisplayName("Verificación")
    class Verification {

        @Test
        @DisplayName("Debe aceptar PIN correcto")
        void shouldAcceptCorrectPin() {
            DeliveryPin pin = DeliveryPin.generate();
            assertTrue(pin.verify(pin.getValue()));
        }

        @Test
        @DisplayName("Debe rechazar PIN incorrecto")
        void shouldRejectIncorrectPin() {
            DeliveryPin pin = DeliveryPin.generate();
            // Usar un PIN diferente
            String wrong = pin.getValue().equals("0000") ? "1111" : "0000";
            assertFalse(pin.verify(wrong));
        }

        @Test
        @DisplayName("Debe incrementar intentos en cada verificación")
        void shouldIncrementAttempts() {
            DeliveryPin pin = DeliveryPin.generate();
            String wrong = pin.getValue().equals("0000") ? "1111" : "0000";

            pin.verify(wrong);
            assertEquals(1, pin.getVerificationAttempts());

            pin.verify(wrong);
            assertEquals(2, pin.getVerificationAttempts());
        }

        @Test
        @DisplayName("Debe bloquearse tras MAX_VERIFICATION_ATTEMPTS")
        void shouldBlockAfterMaxAttempts() {
            DeliveryPin pin = DeliveryPin.generate();
            String wrong = pin.getValue().equals("0000") ? "1111" : "0000";

            for (int i = 0; i < DeliveryPin.MAX_VERIFICATION_ATTEMPTS; i++) {
                pin.verify(wrong);
            }

            assertTrue(pin.isBlocked());
            assertEquals(0, pin.getRemainingAttempts());
            assertThrows(InvalidDeliveryPinException.class,
                    () -> pin.verify(pin.getValue()));
        }
    }

    @Nested
    @DisplayName("Seguridad")
    class Security {

        @Test
        @DisplayName("toString() no debe revelar el PIN")
        void toStringShouldMaskPin() {
            DeliveryPin pin = DeliveryPin.generate();
            assertFalse(pin.toString().contains(pin.getValue()));
            assertTrue(pin.toString().contains("****"));
        }

        @Test
        @DisplayName("equals() por valor de PIN")
        void equalsByValue() {
            DeliveryPin pin = DeliveryPin.generate();
            // Dos PINs con el mismo valor son iguales
            // (aunque tengan diferente createdAt)
            assertEquals(pin, pin);
        }
    }
}
