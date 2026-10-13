-- ═══════════════════════════════════════════════════════════════════════
-- V2__seed_tulcan_data.sql
-- Datos iniciales y de prueba adaptados estrictamente al contexto de
-- Tulcán, Carchi, República del Ecuador.
-- Moneda oficial: Dólares de los Estados Unidos ($ USD)
-- Coordenadas: Área urbana de Tulcán (Lat ~0.8116, Lon -77.7172)
-- Identificación fiscal: RUC estándar ecuatoriano (13 dígitos, prefijo 04)
-- ═══════════════════════════════════════════════════════════════════════

-- ── 1. USUARIOS DEL SISTEMA (TULCÁN, CARCHI) ─────────────────────────
INSERT INTO users (id, email, password_hash, full_name, phone, role, active)
VALUES
    ('00000000-0000-0000-0001-000000000001', 'valeria.narvaez@tulcan.ec', '$2a$10$7EqJtq98hPqEX7fNZaFWoO.8lWkK9aQyQx8aI1F3.pC4oH3L.5t4G', 'Valeria Narváez Guerrón', '+593 99 491 2384', 'CUSTOMER', TRUE),
    ('00000000-0000-0000-0002-000000000001', 'patricio.benavides@lenadortulcan.ec', '$2a$10$7EqJtq98hPqEX7fNZaFWoO.8lWkK9aQyQx8aI1F3.pC4oH3L.5t4G', 'Patricio Benavides Cárdenas', '+593 6 298 0142', 'MERCHANT', TRUE),
    ('00000000-0000-0000-0003-000000000001', 'carlos.mendoza@tulcan.ec', '$2a$10$7EqJtq98hPqEX7fNZaFWoO.8lWkK9aQyQx8aI1F3.pC4oH3L.5t4G', 'Carlos Mendoza Revelo', '+593 99 123 4567', 'DRIVER', TRUE),
    ('00000000-0000-0000-0004-000000000001', 'admin@deliverytulcan.ec', '$2a$10$7EqJtq98hPqEX7fNZaFWoO.8lWkK9aQyQx8aI1F3.pC4oH3L.5t4G', 'Administración GAD Municipal Tulcán', '+593 6 298 0244', 'ADMIN', TRUE)
ON CONFLICT (id) DO NOTHING;

-- ── 2. COMERCIOS DE TULCÁN CON UBICACIÓN POSTGIS ─────────────────────
INSERT INTO merchants (id, owner_id, name, description, address, location, phone, email, category, rating, active)
VALUES
    (
        '11111111-1111-1111-1111-111111111111',
        '00000000-0000-0000-0002-000000000001',
        'Asadero & Parrilladas El Leñador Tulcán',
        'Especialidad en pollo asado con papas pastusas del Carchi y carnes al carbón',
        'Calle Sucre 14-20 y Pichincha, frente al Parque de la Independencia, Tulcán, Carchi',
        ST_SetSRID(ST_MakePoint(-77.7172, 0.8116), 4326),
        '+593 6 298 0142',
        'contacto@lenadortulcan.ec',
        'Restaurantes',
        4.85,
        TRUE
    ),
    (
        '11111111-1111-1111-1111-222222222222',
        '00000000-0000-0000-0002-000000000001',
        'Cafetería & Panadería La Carchi',
        'Café de altura, empanadas de morocho, quimbolitos y pastelería tradicional',
        'Calle 10 de Agosto y Av. Veintimilla, Tulcán, Carchi',
        ST_SetSRID(ST_MakePoint(-77.7190, 0.8105), 4326),
        '+593 6 298 1234',
        'pedidos@cafeterialacarchi.ec',
        'Cafetería & Desayunos',
        4.70,
        TRUE
    ),
    (
        '11111111-1111-1111-1111-333333333333',
        '00000000-0000-0000-0002-000000000001',
        'Farmacia & Botica Cruz del Norte Tulcán',
        'Medicamentos genéricos y de marca, cuidado personal y primeros auxilios',
        'Av. Coral y Calle Bolívar, sector Mercado Central, Tulcán, Carchi',
        ST_SetSRID(ST_MakePoint(-77.7150, 0.8130), 4326),
        '+593 6 298 5678',
        'farmacia@cruzdelnorte.ec',
        'Farmacia / Salud',
        4.90,
        TRUE
    )
ON CONFLICT (id) DO NOTHING;

-- ── 3. PRODUCTOS EN DÓLARES AMERICANOS ($ USD) ────────────────────────
INSERT INTO products (id, merchant_id, name, description, price, category, available)
VALUES
    ('22222222-2222-2222-2222-000000000001', '11111111-1111-1111-1111-111111111111', 'Pollo Asado Entero Familiar', 'Incluye porción grande de papas pastusas, menestra y ensalada fresca', 11.00, 'Platos Fuertes', TRUE),
    ('22222222-2222-2222-2222-000000000002', '11111111-1111-1111-1111-111111111111', 'Medio Pollo Asado con Guarnición', 'Medio pollo asado al leño con papas fritas y ensalada', 6.00, 'Platos Fuertes', TRUE),
    ('22222222-2222-2222-2222-000000000003', '11111111-1111-1111-1111-111111111111', 'Combo Empanadas de Morocho (x3)', 'Empanadas de carne de res con ají tulcaneño', 2.50, 'Entradas', TRUE),
    ('22222222-2222-2222-2222-000000000004', '11111111-1111-1111-1111-111111111111', 'Jarra de Jugo de Mora Silvestre 1L', 'Jugo natural de fruta fresca de los Andes', 2.00, 'Bebidas', TRUE),
    ('22222222-2222-2222-2222-000000000005', '11111111-1111-1111-1111-222222222222', 'Desayuno Tradicional del Carchi', 'Huevos al gusto, queso tierno de Tulcán, pan de casa y café pasado', 3.50, 'Desayunos', TRUE),
    ('22222222-2222-2222-2222-000000000006', '11111111-1111-1111-1111-222222222222', 'Café Pasado Lojano de Altura', 'Taza de café arábica recién filtrado', 1.50, 'Cafetería', TRUE)
ON CONFLICT (id) DO NOTHING;

-- ── 4. REPARTIDOR DE TULCÁN CON PLACAS Y DOCUMENTOS DE ECUADOR ───────
INSERT INTO drivers (
    id, user_id, delivery_level, vehicle_plate, vehicle_type,
    license_number, tax_id, active, current_location, available, rating, total_deliveries
)
VALUES (
    '33333333-3333-3333-3333-111111111111',
    '00000000-0000-0000-0003-000000000001',
    'SENIOR',
    'IC-482Y',                          -- Placa ecuatoriana de motocicleta (Carchi: serie I)
    'MOTORCYCLE',
    'ANT-0401928374',                   -- Licencia Tipo A Agencia Nacional de Tránsito (ANT)
    '0401928374001',                    -- RUC ecuatoriano de persona natural (13 dígitos)
    TRUE,                               -- Repartidor aprobado por administración
    ST_SetSRID(ST_MakePoint(-77.7205, 0.8080), 4326),  -- En ruta por Av. Veintimilla
    TRUE,
    4.95,
    1240
)
ON CONFLICT (id) DO NOTHING;

-- ── 5. PEDIDO DEMO ACTIVO EN TULCÁN (#1048) ───────────────────────────
INSERT INTO orders (
    id, customer_id, merchant_id, driver_id, status,
    pickup_address, pickup_location,
    delivery_address, delivery_location,
    total_amount, delivery_pin_value, delivery_pin_created_at, delivery_pin_expires_at, delivery_pin_attempts, notes
)
VALUES (
    'a47b198c-8f12-4e9b-b6d3-2e40bf1d3902',
    '00000000-0000-0000-0001-000000000001',
    '11111111-1111-1111-1111-111111111111',
    '33333333-3333-3333-3333-111111111111',
    'ON_THE_WAY',
    'Calle Sucre 14-20 y Pichincha, Parque de la Independencia, Tulcán, Carchi',
    ST_SetSRID(ST_MakePoint(-77.7172, 0.8116), 4326),
    'Av. Coral 450 y Av. Manabí, diagonal al Terminal Terrestre, Tulcán, Carchi',
    ST_SetSRID(ST_MakePoint(-77.7245, 0.8035), 4326),
    12.50,                             -- $12.50 USD ($11.00 productos + $1.50 envío)
    '4829',                            -- PIN OTP Anti-Fraude
    NOW(),
    NOW() + INTERVAL '1 hour',
    0,
    'Dejar en recepción de torre norte, timbre 14B. Pagado en USD.'
)
ON CONFLICT (id) DO NOTHING;

-- Ítems del pedido #1048
INSERT INTO order_items (id, order_id, product_id, product_name, quantity, unit_price, subtotal)
VALUES
    ('44444444-4444-4444-4444-000000000001', 'a47b198c-8f12-4e9b-b6d3-2e40bf1d3902', '22222222-2222-2222-2222-000000000001', 'Pollo Asado Entero Familiar', 1, 11.00, 11.00)
ON CONFLICT (id) DO NOTHING;
