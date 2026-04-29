-- Tabela de cotações de insumos
-- Permite comparar preços de fornecedores e calcular economia potencial.
-- farm_id é opcional: null = cotação geral da conta.

CREATE TABLE quotations (
                            id               UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
                            account_id       UUID           NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
                            farm_id          UUID           REFERENCES farms (id) ON DELETE SET NULL,

                            product_name     VARCHAR(200)   NOT NULL,
                            supplier         VARCHAR(200)   NOT NULL,
                            unit_price       DECIMAL(12, 2) NOT NULL,
                            quantity         DECIMAL(12, 4) NOT NULL,
                            unit             VARCHAR(50),

                            quotation_date   DATE           NOT NULL,
                            notes            TEXT,

                            created_at       TIMESTAMP      NOT NULL DEFAULT NOW(),
                            updated_at       TIMESTAMP      NOT NULL DEFAULT NOW()
);

-- Índice para listagem e agrupamento por produto dentro da conta
CREATE INDEX idx_quotations_account_product ON quotations (account_id, product_name);

-- Índice para filtro por lavoura
CREATE INDEX idx_quotations_farm ON quotations (farm_id);