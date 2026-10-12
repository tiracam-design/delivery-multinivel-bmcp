package com.delivery.domain.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para la máquina de estados {@link OrderStatus}.
 */
@DisplayName("OrderStatus — Máquina de Estados")
class OrderStatusTest {

    @Test
    @DisplayName("CREATED permite solo ACCEPTED y CANCELLED")
    void createdTransitions() {
        var allowed = OrderStatus.CREATED.getAllowedTransitions();
        assertEquals(2, allowed.size());
        assertTrue(allowed.contains(OrderStatus.ACCEPTED));
        assertTrue(allowed.contains(OrderStatus.CANCELLED));
    }

    @Test
    @DisplayName("ON_THE_WAY permite solo DELIVERED e INCIDENT_PAUSED")
    void onTheWayTransitions() {
        var allowed = OrderStatus.ON_THE_WAY.getAllowedTransitions();
        assertEquals(2, allowed.size());
        assertTrue(allowed.contains(OrderStatus.DELIVERED));
        assertTrue(allowed.contains(OrderStatus.INCIDENT_PAUSED));
    }

    @Test
    @DisplayName("INCIDENT_PAUSED permite ON_THE_WAY y CANCELLED")
    void incidentPausedTransitions() {
        assertTrue(OrderStatus.INCIDENT_PAUSED.canTransitionTo(OrderStatus.ON_THE_WAY));
        assertTrue(OrderStatus.INCIDENT_PAUSED.canTransitionTo(OrderStatus.CANCELLED));
        assertFalse(OrderStatus.INCIDENT_PAUSED.canTransitionTo(OrderStatus.DELIVERED));
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"DELIVERED", "CANCELLED"})
    @DisplayName("Estados terminales no tienen transiciones permitidas")
    void terminalStatesHaveNoTransitions(OrderStatus terminal) {
        assertTrue(terminal.isTerminal());
        assertFalse(terminal.isActive());
        assertTrue(terminal.getAllowedTransitions().isEmpty());
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class,
            names = {"DELIVERED", "CANCELLED"}, mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Estados no-terminales son activos")
    void nonTerminalStatesAreActive(OrderStatus active) {
        assertTrue(active.isActive());
        assertFalse(active.isTerminal());
    }

    @Test
    @DisplayName("Las transiciones retornadas son inmutables")
    void allowedTransitionsAreImmutable() {
        var transitions = OrderStatus.CREATED.getAllowedTransitions();
        assertThrows(UnsupportedOperationException.class,
                () -> transitions.add(OrderStatus.DELIVERED));
    }
}
