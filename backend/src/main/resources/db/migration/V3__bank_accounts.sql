-- ═══════════════════════════════════════════════════════════════════════
-- V3__bank_accounts.sql
-- Tabla para registrar y actualizar los datos bancarios y códigos QR de
-- cobro de los comercios (ROLE_MERCHANT) y repartidores (ROLE_DRIVER) en Tulcán
-- ═══════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS bank_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    merchant_id UUID REFERENCES merchants(id) ON DELETE CASCADE,
    driver_id UUID REFERENCES drivers(id) ON DELETE CASCADE,
    bank_name VARCHAR(100) NOT NULL DEFAULT 'Banco Pichincha',
    account_type VARCHAR(50) NOT NULL DEFAULT 'Ahorros',
    account_number VARCHAR(50) NOT NULL,
    account_holder_id VARCHAR(50) NOT NULL,
    account_holder_name VARCHAR(150) NOT NULL,
    qr_code_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_bank_accounts_user_id ON bank_accounts(user_id);
CREATE INDEX IF NOT EXISTS idx_bank_accounts_merchant_id ON bank_accounts(merchant_id);
CREATE INDEX IF NOT EXISTS idx_bank_accounts_driver_id ON bank_accounts(driver_id);

-- Sembrar cuentas bancarias oficiales iniciales para los comercios y repartidores de Tulcán
INSERT INTO bank_accounts (id, user_id, merchant_id, bank_name, account_type, account_number, account_holder_id, account_holder_name, qr_code_url)
VALUES
    (
        'aaaaaaaa-aaaa-aaaa-aaaa-111111111111',
        '00000000-0000-0000-0002-000000000001',
        '11111111-1111-1111-1111-111111111111',
        'Banco Pichincha',
        'Ahorros',
        '2201928374',
        '0491500042001',
        'Patricio Benavides Cárdenas (Asadero El Leñador)',
        'https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=DEUNA_TULCAN_LENADOR_2201928374'
    ),
    (
        'aaaaaaaa-aaaa-aaaa-aaaa-222222222222',
        '00000000-0000-0000-0002-000000000001',
        '11111111-1111-1111-1111-222222222222',
        'Cooperativa Tulcán',
        'Ahorros',
        '1040582910',
        '0401829471',
        'Elena Villarreal Montenegro (Cafetería La Carchi)',
        'https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=DEUNA_TULCAN_LACARCHI_1040582910'
    ),
    (
        'aaaaaaaa-aaaa-aaaa-aaaa-333333333333',
        '00000000-0000-0000-0003-000000000001',
        NULL,
        'Banco Pichincha',
        'Ahorros',
        '2208194720',
        '0401837291',
        'Carlos Mendoza Revelo (Repartidor Tulcán)',
        'https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=DEUNA_TULCAN_DRIVER_CARLOS'
    )
ON CONFLICT (id) DO NOTHING;
