package com.delivery.domain.enums;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Máquina de estados finitos para el ciclo de vida de un pedido.
 *
 * <p>Cada constante define el conjunto de estados sucesores válidos,
 * implementando el flujo de la BMCP v1.0.0. Las transiciones que no
 * estén explícitamente declaradas están <strong>prohibidas</strong>.</p>
 *
 * <h3>Diagrama de transiciones:</h3>
 * <pre>
 *   CREATED ──────────→ ACCEPTED ──────────→ IN_PREPARATION
 *     │                    │                       │
 *     ↓                    ↓                       ↓
 *  CANCELLED           CANCELLED               CANCELLED
 *                                                  │
 *                                                  ↓
 *                                          READY_FOR_PICKUP
 *                                                  │
 *                                           ┌──────┴──────┐
 *                                           ↓             ↓
 *                                      ON_THE_WAY     CANCELLED
 *                                           │
 *                                    ┌──────┴──────┐
 *                                    ↓             ↓
 *                               DELIVERED   INCIDENT_PAUSED
 *                                                  │
 *                                           ┌──────┴──────┐
 *                                           ↓             ↓
 *                                      ON_THE_WAY     CANCELLED
 * </pre>
 *
 * <h3>Semántica de cada estado:</h3>
 * <ul>
 *   <li>{@code CREATED}          — pedido registrado, pendiente de aceptación por el comercio</li>
 *   <li>{@code ACCEPTED}         — comercio aceptó el pedido, comienza preparación</li>
 *   <li>{@code IN_PREPARATION}   — pedido siendo preparado en el comercio</li>
 *   <li>{@code READY_FOR_PICKUP} — pedido listo, esperando al repartidor</li>
 *   <li>{@code ON_THE_WAY}       — repartidor recogió el pedido y va hacia el destino</li>
 *   <li>{@code DELIVERED}        — entrega confirmada con PIN OTP (estado terminal)</li>
 *   <li>{@code CANCELLED}        — pedido cancelado (estado terminal)</li>
 *   <li>{@code INCIDENT_PAUSED}  — incidente en ruta (problema con dirección, clima, etc.)</li>
 * </ul>
 *
 * @since 1.0.0
 */
public enum OrderStatus {

    // ── Estados del flujo principal ──────────────────────────────────

    /**
     * Pedido recién creado. Puede ser aceptado por el comercio o
     * cancelado por el cliente.
     */
    CREATED {
        @Override
        protected Set<OrderStatus> allowedTransitions() {
            return EnumSet.of(ACCEPTED, CANCELLED);
        }
    },

    /**
     * Comercio aceptó el pedido. Puede pasar a preparación o
     * ser cancelado.
     */
    ACCEPTED {
        @Override
        protected Set<OrderStatus> allowedTransitions() {
            return EnumSet.of(IN_PREPARATION, CANCELLED);
        }
    },

    /**
     * Pedido en preparación por el comercio. Puede quedar listo
     * para recoger o ser cancelado.
     */
    IN_PREPARATION {
        @Override
        protected Set<OrderStatus> allowedTransitions() {
            return EnumSet.of(READY_FOR_PICKUP, CANCELLED);
        }
    },

    /**
     * Pedido listo para ser recogido por el repartidor.
     * Puede pasar a "en camino" o ser cancelado.
     */
    READY_FOR_PICKUP {
        @Override
        protected Set<OrderStatus> allowedTransitions() {
            return EnumSet.of(ON_THE_WAY, CANCELLED);
        }
    },

    /**
     * Repartidor en ruta hacia el destino. Puede entregar
     * exitosamente o pausar por incidente.
     */
    ON_THE_WAY {
        @Override
        protected Set<OrderStatus> allowedTransitions() {
            return EnumSet.of(DELIVERED, INCIDENT_PAUSED);
        }
    },

    // ── Estados terminales ──────────────────────────────────────────

    /**
     * Entrega confirmada con PIN OTP. <strong>Estado terminal.</strong>
     * No se permiten más transiciones.
     */
    DELIVERED {
        @Override
        protected Set<OrderStatus> allowedTransitions() {
            return EnumSet.noneOf(OrderStatus.class);
        }
    },

    /**
     * Pedido cancelado. <strong>Estado terminal.</strong>
     * No se permiten más transiciones.
     */
    CANCELLED {
        @Override
        protected Set<OrderStatus> allowedTransitions() {
            return EnumSet.noneOf(OrderStatus.class);
        }
    },

    // ── Estado de contingencia ──────────────────────────────────────

    /**
     * Incidente durante la entrega (dirección incorrecta, clima
     * extremo, bloqueo vial, etc.). Puede retomar la ruta
     * o cancelarse definitivamente.
     */
    INCIDENT_PAUSED {
        @Override
        protected Set<OrderStatus> allowedTransitions() {
            return EnumSet.of(ON_THE_WAY, CANCELLED);
        }
    };

    // ── API pública ─────────────────────────────────────────────────

    /**
     * Conjunto inmutable de transiciones válidas desde este estado.
     * Método template implementado por cada constante del enum.
     *
     * @return set no modificable de estados sucesores permitidos
     */
    protected abstract Set<OrderStatus> allowedTransitions();

    /**
     * Retorna las transiciones permitidas como un {@link Set} inmutable.
     *
     * <p>Método público para consumo externo (mensajes de error,
     * documentación de API, logs).</p>
     *
     * @return set inmutable de estados sucesores válidos
     */
    public Set<OrderStatus> getAllowedTransitions() {
        return Collections.unmodifiableSet(allowedTransitions());
    }

    /**
     * Valida si la transición desde este estado al estado destino
     * está permitida por la máquina de estados.
     *
     * @param target estado destino
     * @return {@code true} si la transición es válida
     */
    public boolean canTransitionTo(OrderStatus target) {
        return allowedTransitions().contains(target);
    }

    /**
     * Indica si este estado es un estado terminal (no admite más
     * transiciones).
     *
     * @return {@code true} si el pedido alcanzó un estado final
     */
    public boolean isTerminal() {
        return allowedTransitions().isEmpty();
    }

    /**
     * Indica si el pedido se encuentra en un estado activo
     * (no terminal).
     *
     * @return {@code true} si aún puede transicionar a otro estado
     */
    public boolean isActive() {
        return !isTerminal();
    }
}
