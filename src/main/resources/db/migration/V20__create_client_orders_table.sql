CREATE TABLE client_orders (
    id                   UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id           UUID           NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
    client_id            UUID           NOT NULL REFERENCES trading_clients (id),
    order_date           DATE           NOT NULL,
    client_price_per_kg  DECIMAL(12, 2) NOT NULL,
    status               VARCHAR(10)    NOT NULL DEFAULT 'OPEN',
    notes                TEXT,
    created_at           TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_client_orders_account ON client_orders (account_id);
CREATE INDEX idx_client_orders_client  ON client_orders (client_id);
