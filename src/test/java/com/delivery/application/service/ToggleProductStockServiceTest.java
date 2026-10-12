package com.delivery.application.service;

import com.delivery.domain.model.Product;
import com.delivery.domain.port.output.ProductRepositoryPort;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ToggleProductStockService — Tests Unitarios")
class ToggleProductStockServiceTest {

    @Mock
    private ProductRepositoryPort productRepository;

    private ToggleProductStockService service;

    private final UUID productId = UUID.randomUUID();
    private final UUID merchantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ToggleProductStockService(productRepository);
    }

    @Test
    @DisplayName("toggleAvailability: debe delegar actualización atómica en el repositorio")
    void shouldToggleAvailabilitySuccessfully() {
        when(productRepository.toggleAvailability(productId)).thenReturn(false);

        boolean result = service.toggleAvailability(productId);

        assertFalse(result);
        verify(productRepository).toggleAvailability(productId);
    }

    @Test
    @DisplayName("toggleAvailability: fallback a modelo de dominio si falla actualización atómica")
    void shouldFallbackToDomainModelWhenAtomicFails() {
        Product product = new Product(
                productId, merchantId, "Hamburguesa Clásica", "Deliciosa hamburguesa",
                new BigDecimal("85.00"), "Comida Rápida", null, true, Instant.now(), Instant.now()
        );

        when(productRepository.toggleAvailability(productId))
                .thenThrow(new RuntimeException("Simulated atomic failure"));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        boolean result = service.toggleAvailability(productId);

        assertFalse(result, "Debe alternar de true a false");
        verify(productRepository).save(product);
    }
}
