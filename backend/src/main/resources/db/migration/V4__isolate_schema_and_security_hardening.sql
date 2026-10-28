-- ═══════════════════════════════════════════════════════════════════════
-- V4__isolate_schema_and_security_hardening.sql
-- ═══════════════════════════════════════════════════════════════════════
-- 1. Aislamiento de Esquema de Producción:
--    Mueve todas las tablas operativas de la plataforma fuera del esquema
--    por defecto 'public' al esquema privado 'bmcp_app'.
-- 2. Hardening y Ampliación de Cuentas de Cobro (bank_accounts):
--    Añade campos de cobro QR (deuna_qr_url), estado (is_active) y
--    restricciones a nivel de columna para datos sensibles.
-- 3. Política de Mínimo Privilegio (Least Privilege Principle):
--    Crea el rol de aplicación 'bmcp_app_user' con permisos restringidos
--    (SELECT, INSERT, UPDATE, DELETE) y revoca privilegios de modificación
--    sobre los esquemas públicos y de PostGIS (tiger, tiger_data, topology).
-- ═══════════════════════════════════════════════════════════════════════

-- ── 1. AISLAMIENTO DE ESQUEMA PRIVADO (bmcp_app) ─────────────────────
CREATE SCHEMA IF NOT EXISTS bmcp_app;

-- Mover tablas operativas de 'public' a 'bmcp_app' sin pérdida de datos
ALTER TABLE IF EXISTS public.users SET SCHEMA bmcp_app;
ALTER TABLE IF EXISTS public.merchants SET SCHEMA bmcp_app;
ALTER TABLE IF EXISTS public.products SET SCHEMA bmcp_app;
ALTER TABLE IF EXISTS public.drivers SET SCHEMA bmcp_app;
ALTER TABLE IF EXISTS public.orders SET SCHEMA bmcp_app;
ALTER TABLE IF EXISTS public.order_items SET SCHEMA bmcp_app;
ALTER TABLE IF EXISTS public.order_status_history SET SCHEMA bmcp_app;
ALTER TABLE IF EXISTS public.driver_locations SET SCHEMA bmcp_app;
ALTER TABLE IF EXISTS public.delivery_zones SET SCHEMA bmcp_app;
ALTER TABLE IF EXISTS public.bank_accounts SET SCHEMA bmcp_app;

-- ── 2. ESTRUCTURA Y HARDENING DE bank_accounts ───────────────────────
-- Ampliar longitud de columnas para permitir tokens cifrados de forma holgada
ALTER TABLE bmcp_app.bank_accounts ALTER COLUMN account_number TYPE VARCHAR(255);
ALTER TABLE bmcp_app.bank_accounts ALTER COLUMN account_holder_id TYPE VARCHAR(255);

-- Garantizar campos de cobro y compatibilidad
ALTER TABLE bmcp_app.bank_accounts ADD COLUMN IF NOT EXISTS deuna_qr_url TEXT;
UPDATE bmcp_app.bank_accounts SET deuna_qr_url = qr_code_url WHERE deuna_qr_url IS NULL;

ALTER TABLE bmcp_app.bank_accounts ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- Restricciones de validación a nivel de columna para datos sensibles
ALTER TABLE bmcp_app.bank_accounts DROP CONSTRAINT IF EXISTS chk_bank_accounts_number_len;
ALTER TABLE bmcp_app.bank_accounts ADD CONSTRAINT chk_bank_accounts_number_len 
    CHECK (length(trim(account_number)) >= 5);

ALTER TABLE bmcp_app.bank_accounts DROP CONSTRAINT IF EXISTS chk_bank_accounts_holder_id_len;
ALTER TABLE bmcp_app.bank_accounts ADD CONSTRAINT chk_bank_accounts_holder_id_len 
    CHECK (length(trim(account_holder_id)) >= 5);

ALTER TABLE bmcp_app.bank_accounts DROP CONSTRAINT IF EXISTS chk_bank_accounts_bank_name_len;
ALTER TABLE bmcp_app.bank_accounts ADD CONSTRAINT chk_bank_accounts_bank_name_len 
    CHECK (length(trim(bank_name)) >= 2);

ALTER TABLE bmcp_app.bank_accounts DROP CONSTRAINT IF EXISTS chk_bank_accounts_holder_name_len;
ALTER TABLE bmcp_app.bank_accounts ADD CONSTRAINT chk_bank_accounts_holder_name_len 
    CHECK (length(trim(account_holder_name)) >= 3);

-- Índices optimizados en bmcp_app.bank_accounts
CREATE INDEX IF NOT EXISTS idx_bank_accounts_is_active ON bmcp_app.bank_accounts (is_active);

-- ── 3. POLÍTICA DE ROLES Y MÍNIMO PRIVILEGIO (Least Privilege) ────────
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'bmcp_app_user') THEN
        CREATE ROLE bmcp_app_user WITH LOGIN PASSWORD 'bmcp_app_pass_2026';
    END IF;
END
$$;

-- Revocar permisos de creación y modificación general en public
REVOKE CREATE ON SCHEMA public FROM PUBLIC;

-- Revocar privilegios de modificación sobre esquemas del sistema PostGIS (si existen en el host)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = 'tiger') THEN
        EXECUTE 'REVOKE ALL ON SCHEMA tiger FROM PUBLIC, bmcp_app_user';
        REVOKE ALL ON ALL TABLES IN SCHEMA tiger FROM PUBLIC, bmcp_app_user;
        REVOKE ALL ON ALL ROUTINES IN SCHEMA tiger FROM PUBLIC, bmcp_app_user;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = 'tiger_data') THEN
        EXECUTE 'REVOKE ALL ON SCHEMA tiger_data FROM PUBLIC, bmcp_app_user';
        REVOKE ALL ON ALL TABLES IN SCHEMA tiger_data FROM PUBLIC, bmcp_app_user;
        REVOKE ALL ON ALL ROUTINES IN SCHEMA tiger_data FROM PUBLIC, bmcp_app_user;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = 'topology') THEN
        EXECUTE 'REVOKE ALL ON SCHEMA topology FROM PUBLIC, bmcp_app_user';
        REVOKE ALL ON ALL TABLES IN SCHEMA topology FROM PUBLIC, bmcp_app_user;
        REVOKE ALL ON ALL ROUTINES IN SCHEMA topology FROM PUBLIC, bmcp_app_user;
    END IF;
END
$$;

-- Conceder mínimos privilegios al rol de aplicación (DML en bmcp_app)
GRANT USAGE ON SCHEMA bmcp_app TO bmcp_app_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA bmcp_app TO bmcp_app_user;
GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA bmcp_app TO bmcp_app_user;

ALTER DEFAULT PRIVILEGES IN SCHEMA bmcp_app GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO bmcp_app_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA bmcp_app GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO bmcp_app_user;

-- Otorgar únicamente lectura sobre objetos espaciales de PostGIS en public
GRANT USAGE ON SCHEMA public TO bmcp_app_user;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO bmcp_app_user;

-- Garantizar propiedad y control total del esquema al usuario de migraciones/DBA si existe
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'delivery_user') THEN
        GRANT ALL PRIVILEGES ON SCHEMA bmcp_app TO delivery_user;
        GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA bmcp_app TO delivery_user;
        GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA bmcp_app TO delivery_user;
    END IF;
END
$$;
