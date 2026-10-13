-- ═══════════════════════════════════════════════════════════════════════
-- V1__init_schema.sql
-- Migración Flyway: Esquema completo — Sistema de Delivery Multinivel
-- BMCP v1.0.0 — Llaves UUID, PostGIS SRID 4326, Índices GIST
-- ═══════════════════════════════════════════════════════════════════════

-- ── Extensiones ─────────────────────────────────────────────────────
CREATE EXTENSION IF NOT EXISTS "pgcrypto";   -- gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS "postgis";    -- tipos GEOMETRY, ST_*

-- ═════════════════════════════════════════════════════════════════════
--  USERS — Tabla base de usuarios (todos los roles)
-- ═════════════════════════════════════════════════════════════════════
CREATE TABLE users (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255)    NOT NULL UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,
    full_name       VARCHAR(150)    NOT NULL,
    phone           VARCHAR(20),
    role            VARCHAR(30)     NOT NULL,
        -- Roles: ADMIN, CUSTOMER, MERCHANT, DRIVER, DISPATCHER
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_role  ON users (role);

-- ═════════════════════════════════════════════════════════════════════
--  MERCHANTS — Comercios / Restaurantes
-- ═════════════════════════════════════════════════════════════════════
CREATE TABLE merchants (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id        UUID            NOT NULL REFERENCES users(id),
    name            VARCHAR(150)    NOT NULL,
    description     TEXT,
    address         VARCHAR(500)    NOT NULL,
    location        GEOMETRY(Point, 4326) NOT NULL,  -- Ubicación PostGIS
    phone           VARCHAR(20),
    email           VARCHAR(255),
    category        VARCHAR(50),
    logo_url        VARCHAR(500),
    rating          DECIMAL(3, 2)   NOT NULL DEFAULT 0.00,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_merchants_location ON merchants USING GIST (location);
CREATE INDEX idx_merchants_owner    ON merchants (owner_id);
CREATE INDEX idx_merchants_category ON merchants (category);

-- ═════════════════════════════════════════════════════════════════════
--  PRODUCTS — Productos ofrecidos por comercios
-- ═════════════════════════════════════════════════════════════════════
CREATE TABLE products (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id     UUID            NOT NULL REFERENCES merchants(id),
    name            VARCHAR(150)    NOT NULL,
    description     TEXT,
    price           DECIMAL(12, 2)  NOT NULL CHECK (price > 0),
    category        VARCHAR(50),
    image_url       VARCHAR(500),
    available       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_products_merchant  ON products (merchant_id);
CREATE INDEX idx_products_category  ON products (category);

-- ═════════════════════════════════════════════════════════════════════
--  DRIVERS — Perfil extendido del repartidor (multinivel)
-- ═════════════════════════════════════════════════════════════════════
CREATE TABLE drivers (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID            NOT NULL UNIQUE REFERENCES users(id),
    delivery_level      VARCHAR(30)     NOT NULL DEFAULT 'JUNIOR',
        -- Niveles multinivel: JUNIOR, SENIOR, MASTER
    vehicle_plate       VARCHAR(20),
    vehicle_type        VARCHAR(30),
        -- Tipos: MOTORCYCLE, CAR, VAN, TRUCK
    license_number      VARCHAR(50),
    tax_id              VARCHAR(50),
        -- RUC / RFC / Identificación tributaria
    active              BOOLEAN         NOT NULL DEFAULT FALSE,
        -- Estado de aprobación por administración (REQ-SEC-002)
    current_location    GEOMETRY(Point, 4326),
    available           BOOLEAN         NOT NULL DEFAULT FALSE,
    rating              DECIMAL(3, 2)   NOT NULL DEFAULT 0.00,
    total_deliveries    INTEGER         NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_drivers_user_id          ON drivers (user_id);
CREATE INDEX idx_drivers_current_location ON drivers USING GIST (current_location);
CREATE INDEX idx_drivers_available        ON drivers (available) WHERE available = TRUE;

-- ═════════════════════════════════════════════════════════════════════
--  ORDERS — Pedidos (con PIN anti-fraude embebido)
-- ═════════════════════════════════════════════════════════════════════
CREATE TABLE orders (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id             UUID            NOT NULL REFERENCES users(id),
    merchant_id             UUID            NOT NULL REFERENCES merchants(id),
    driver_id               UUID            REFERENCES drivers(id),
    status                  VARCHAR(30)     NOT NULL DEFAULT 'CREATED',
        -- Estados BMCP: CREATED, ACCEPTED, IN_PREPARATION,
        -- READY_FOR_PICKUP, ON_THE_WAY, DELIVERED,
        -- CANCELLED, INCIDENT_PAUSED

    -- Dirección de recogida
    pickup_address          VARCHAR(500)    NOT NULL,
    pickup_location         GEOMETRY(Point, 4326),

    -- Dirección de entrega
    delivery_address        VARCHAR(500)    NOT NULL,
    delivery_location       GEOMETRY(Point, 4326),

    -- Estimaciones
    estimated_distance_km   DOUBLE PRECISION,
    estimated_duration_min  INTEGER,

    -- Monto
    total_amount            DECIMAL(12, 2)  NOT NULL CHECK (total_amount > 0),

    -- PIN anti-fraude (OTP 4 dígitos, embebido como Value Object)
    delivery_pin_value      VARCHAR(4),
    delivery_pin_created_at TIMESTAMPTZ,
    delivery_pin_expires_at TIMESTAMPTZ,
    delivery_pin_attempts   INTEGER         NOT NULL DEFAULT 0,

    -- Metadatos
    notes                   TEXT,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_pickup_location   ON orders USING GIST (pickup_location);
CREATE INDEX idx_orders_delivery_location ON orders USING GIST (delivery_location);
CREATE INDEX idx_orders_status            ON orders (status);
CREATE INDEX idx_orders_customer_id       ON orders (customer_id);
CREATE INDEX idx_orders_merchant_id       ON orders (merchant_id);
CREATE INDEX idx_orders_driver_id         ON orders (driver_id);

-- ═════════════════════════════════════════════════════════════════════
--  ORDER_ITEMS — Líneas de detalle del pedido
-- ═════════════════════════════════════════════════════════════════════
CREATE TABLE order_items (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID            NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id      UUID            NOT NULL REFERENCES products(id),
    product_name    VARCHAR(150)    NOT NULL,  -- Desnormalizado para historial
    quantity        INTEGER         NOT NULL CHECK (quantity > 0),
    unit_price      DECIMAL(12, 2)  NOT NULL CHECK (unit_price >= 0),
    subtotal        DECIMAL(12, 2)  NOT NULL CHECK (subtotal >= 0),
    notes           TEXT,

    CONSTRAINT uq_order_product UNIQUE (order_id, product_id)
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);

-- ═════════════════════════════════════════════════════════════════════
--  ORDER_STATUS_HISTORY — Auditoría de transiciones de estado
-- ═════════════════════════════════════════════════════════════════════
CREATE TABLE order_status_history (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID            NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    from_status     VARCHAR(30),     -- NULL para el estado inicial (CREATED)
    to_status       VARCHAR(30)     NOT NULL,
    changed_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    reason          TEXT
);

CREATE INDEX idx_status_history_order ON order_status_history (order_id);

-- ═════════════════════════════════════════════════════════════════════
--  DRIVER_LOCATIONS — Tracking GPS en tiempo real
-- ═════════════════════════════════════════════════════════════════════
CREATE TABLE driver_locations (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    driver_id       UUID            NOT NULL REFERENCES drivers(id),
    location        GEOMETRY(Point, 4326) NOT NULL,
    heading         DOUBLE PRECISION,    -- Dirección en grados (0-360)
    speed           DOUBLE PRECISION,    -- Velocidad en km/h
    recorded_at     TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_driver_locations_driver   ON driver_locations (driver_id);
CREATE INDEX idx_driver_locations_location ON driver_locations USING GIST (location);
CREATE INDEX idx_driver_locations_time     ON driver_locations (driver_id, recorded_at DESC);

-- ═════════════════════════════════════════════════════════════════════
--  DELIVERY_ZONES — Zonas de cobertura (polígonos PostGIS)
-- ═════════════════════════════════════════════════════════════════════
CREATE TABLE delivery_zones (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(100)    NOT NULL,
    boundary        GEOMETRY(Polygon, 4326) NOT NULL,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_delivery_zones_boundary ON delivery_zones USING GIST (boundary);
