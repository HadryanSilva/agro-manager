-- Pedidos de venda de um lote.
-- Representa a negociação com um comprador: data, preço por Kg acordado.
-- O volume vendido é derivado da soma de lot_sale_trucks.

CREATE TABLE lot_sales (
    id            UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id    UUID           NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
    lot_id        UUID           NOT NULL REFERENCES purchase_lots (id) ON DELETE CASCADE,

    buyer_name    VARCHAR(150)   NOT NULL,
    sale_date     DATE           NOT NULL,
    price_per_kg  DECIMAL(10, 4) NOT NULL,

    notes         TEXT,

    created_at    TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_lot_sales_account      ON lot_sales (account_id);
CREATE INDEX idx_lot_sales_lot          ON lot_sales (lot_id);
CREATE INDEX idx_lot_sales_account_date ON lot_sales (account_id, sale_date DESC);
