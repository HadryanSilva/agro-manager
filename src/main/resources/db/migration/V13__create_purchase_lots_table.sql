-- Lotes de compra do modo atravessador.
-- Cada lote representa uma negociação com um fornecedor em uma data específica.
-- O total em Kg é derivado da soma de purchase_trucks — não armazenado aqui.

CREATE TABLE purchase_lots (
    id            UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id    UUID           NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
    supplier_id   UUID           NOT NULL REFERENCES trading_suppliers (id),

    purchase_date DATE           NOT NULL,
    price_per_kg  DECIMAL(10, 4) NOT NULL,

    -- Status do lote: OPEN = ainda há estoque disponível, CLOSED = totalmente vendido ou encerrado manualmente
    status        VARCHAR(10)    NOT NULL DEFAULT 'OPEN',

    notes         TEXT,

    created_at    TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_purchase_lots_account          ON purchase_lots (account_id);
CREATE INDEX idx_purchase_lots_account_status   ON purchase_lots (account_id, status);
CREATE INDEX idx_purchase_lots_supplier         ON purchase_lots (supplier_id);
CREATE INDEX idx_purchase_lots_account_date     ON purchase_lots (account_id, purchase_date DESC);
