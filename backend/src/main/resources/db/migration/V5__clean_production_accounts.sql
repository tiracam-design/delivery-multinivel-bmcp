-- ═══════════════════════════════════════════════════════════════════════
-- V5__clean_production_accounts.sql
-- ═══════════════════════════════════════════════════════════════════════
-- Limpieza de cuentas sintéticas y estandarización a exactamente 4 cuentas
-- base de producción y prueba en el esquema bmcp_app:
--   1. ROLE_ADMIN    -> admin@deliverytulcan.ec (Administración GAD Municipal Tulcán)
--   2. ROLE_MERCHANT -> patricio.benavides@lenadortulcan.ec (Asadero El Leñador)
--   3. ROLE_DRIVER   -> carlos.mendoza@tulcan.ec (Carlos Mendoza Revelo)
--   4. ROLE_CLIENT   -> valeria.narvaez@tulcan.ec (Valeria Narváez Guerrón)
-- ═══════════════════════════════════════════════════════════════════════

-- 1. Eliminar pedidos sintéticos y huérfanos que no pertenezcan a las 4 cuentas oficiales
DELETE FROM bmcp_app.order_items WHERE order_id IN (
    SELECT id FROM bmcp_app.orders WHERE customer_id NOT IN (
        '00000000-0000-0000-0001-000000000001',
        '00000000-0000-0000-0002-000000000001',
        '00000000-0000-0000-0003-000000000001',
        '00000000-0000-0000-0004-000000000001'
    )
);

DELETE FROM bmcp_app.order_status_history WHERE order_id IN (
    SELECT id FROM bmcp_app.orders WHERE customer_id NOT IN (
        '00000000-0000-0000-0001-000000000001',
        '00000000-0000-0000-0002-000000000001',
        '00000000-0000-0000-0003-000000000001',
        '00000000-0000-0000-0004-000000000001'
    )
);

DELETE FROM bmcp_app.orders WHERE customer_id NOT IN (
    '00000000-0000-0000-0001-000000000001',
    '00000000-0000-0000-0002-000000000001',
    '00000000-0000-0000-0003-000000000001',
    '00000000-0000-0000-0004-000000000001'
);

-- 2. Limpiar registros en drivers, merchants o bank_accounts no asociados a las 4 cuentas base
DELETE FROM bmcp_app.bank_accounts WHERE user_id IS NOT NULL AND user_id NOT IN (
    '00000000-0000-0000-0001-000000000001',
    '00000000-0000-0000-0002-000000000001',
    '00000000-0000-0000-0003-000000000001',
    '00000000-0000-0000-0004-000000000001'
);

DELETE FROM bmcp_app.drivers WHERE user_id NOT IN (
    '00000000-0000-0000-0001-000000000001',
    '00000000-0000-0000-0002-000000000001',
    '00000000-0000-0000-0003-000000000001',
    '00000000-0000-0000-0004-000000000001'
);

DELETE FROM bmcp_app.merchants WHERE owner_id NOT IN (
    '00000000-0000-0000-0001-000000000001',
    '00000000-0000-0000-0002-000000000001',
    '00000000-0000-0000-0003-000000000001',
    '00000000-0000-0000-0004-000000000001'
);

-- 3. Eliminar todos los usuarios sintéticos / de prueba adicionales
DELETE FROM bmcp_app.users WHERE id NOT IN (
    '00000000-0000-0000-0001-000000000001',
    '00000000-0000-0000-0002-000000000001',
    '00000000-0000-0000-0003-000000000001',
    '00000000-0000-0000-0004-000000000001'
);

-- 4. Asegurar la consistencia exacta de las 4 cuentas oficiales requeridas
-- Hash BCrypt estándar para 'password123' / 'tulcan2026'
-- 4.1 ROLE_CLIENT
INSERT INTO bmcp_app.users (id, email, password_hash, full_name, phone, role, active, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0001-000000000001',
    'valeria.narvaez@tulcan.ec',
    '$2a$10$7EqJtq98hPqEX7fNZaFWoO.8lWkK9aQyQx8aI1F3.pC4oH3L.5t4G',
    'Valeria Narváez Guerrón',
    '+593 99 491 2384',
    'CUSTOMER',
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT (id) DO UPDATE SET
    email = EXCLUDED.email,
    full_name = EXCLUDED.full_name,
    phone = EXCLUDED.phone,
    role = EXCLUDED.role,
    active = TRUE,
    updated_at = NOW();

-- 4.2 ROLE_MERCHANT
INSERT INTO bmcp_app.users (id, email, password_hash, full_name, phone, role, active, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0002-000000000001',
    'patricio.benavides@lenadortulcan.ec',
    '$2a$10$7EqJtq98hPqEX7fNZaFWoO.8lWkK9aQyQx8aI1F3.pC4oH3L.5t4G',
    'Patricio Benavides Cárdenas',
    '+593 6 298 0142',
    'MERCHANT',
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT (id) DO UPDATE SET
    email = EXCLUDED.email,
    full_name = EXCLUDED.full_name,
    phone = EXCLUDED.phone,
    role = EXCLUDED.role,
    active = TRUE,
    updated_at = NOW();

-- 4.3 ROLE_DRIVER
INSERT INTO bmcp_app.users (id, email, password_hash, full_name, phone, role, active, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0003-000000000001',
    'carlos.mendoza@tulcan.ec',
    '$2a$10$7EqJtq98hPqEX7fNZaFWoO.8lWkK9aQyQx8aI1F3.pC4oH3L.5t4G',
    'Carlos Mendoza Revelo',
    '+593 99 123 4567',
    'DRIVER',
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT (id) DO UPDATE SET
    email = EXCLUDED.email,
    full_name = EXCLUDED.full_name,
    phone = EXCLUDED.phone,
    role = EXCLUDED.role,
    active = TRUE,
    updated_at = NOW();

-- 4.4 ROLE_ADMIN
INSERT INTO bmcp_app.users (id, email, password_hash, full_name, phone, role, active, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0004-000000000001',
    'admin@deliverytulcan.ec',
    '$2a$10$7EqJtq98hPqEX7fNZaFWoO.8lWkK9aQyQx8aI1F3.pC4oH3L.5t4G',
    'Administración GAD Municipal Tulcán',
    '+593 6 298 0244',
    'ADMIN',
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT (id) DO UPDATE SET
    email = EXCLUDED.email,
    full_name = EXCLUDED.full_name,
    phone = EXCLUDED.phone,
    role = EXCLUDED.role,
    active = TRUE,
    updated_at = NOW();
