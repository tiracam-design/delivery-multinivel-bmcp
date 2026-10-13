package com.delivery.domain.enums;

/**
 * Roles de usuario en el Sistema de Delivery Multinivel.
 *
 * <p>Define los perfiles de acceso y responsabilidades dentro
 * de la plataforma.</p>
 *
 * @since 1.0.0
 */
public enum UserRole {

    /** Administrador del sistema — acceso total */
    ADMIN,

    /** Repartidor — recoge y entrega pedidos */
    DRIVER,

    /** Cliente — realiza pedidos */
    CUSTOMER,

    /** Despachador — coordina asignación de repartidores */
    DISPATCHER,

    /** Comercio/Restaurante — recibe y prepara pedidos */
    MERCHANT
}
