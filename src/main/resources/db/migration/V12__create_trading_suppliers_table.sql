-- Cadastro de fornecedores do modo comprador.
-- Cada fornecedor é um produtor de quem o comprador compra melancia.
-- Escopo por conta (account_id) — cada conta mantém sua própria lista.

CREATE TABLE trading_suppliers (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id  UUID         NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,

    name        VARCHAR(150) NOT NULL,
    phone       VARCHAR(20),
    city        VARCHAR(100),
    notes       TEXT,

    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Índice principal para listagem e busca por nome dentro da conta
CREATE INDEX idx_trading_suppliers_account      ON trading_suppliers (account_id);
CREATE INDEX idx_trading_suppliers_account_name ON trading_suppliers (account_id, name);
