CREATE TABLE trading_clients (
    id         UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID           NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
    name       VARCHAR(150)   NOT NULL,
    phone      VARCHAR(20)    NOT NULL,
    city       VARCHAR(100),
    notes      TEXT,
    created_at TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_trading_clients_account ON trading_clients (account_id);
