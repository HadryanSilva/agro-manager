-- Criação da tabela de lavouras
-- Cada lavoura pertence a uma conta e representa um ciclo de plantio de melancia

CREATE TABLE farms (
                       id                  UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
                       account_id          UUID           NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,

    -- Identificação
                       name                VARCHAR(150)   NOT NULL,

    -- Área (valor + unidade separados para permitir conversão futura)
                       area_value          DECIMAL(10, 2) NOT NULL,
                       area_unit           VARCHAR(10)    NOT NULL,

    -- Arrendamento
                       lessor_name         VARCHAR(150),
                       lease_start_date    DATE,
                       lease_end_date      DATE,
                       lease_value         DECIMAL(12, 2),

    -- Período de plantio
                       planting_start_date DATE,
                       planting_end_date   DATE,

    -- Período de colheita
                       harvest_start_date  DATE,
                       harvest_end_date    DATE,

    -- Cancelamento e observações
                       cancelled           BOOLEAN        NOT NULL DEFAULT FALSE,
                       notes               TEXT,

                       created_at          TIMESTAMP      NOT NULL DEFAULT NOW(),
                       updated_at          TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_farms_account ON farms (account_id);
CREATE INDEX idx_farms_account_cancelled ON farms (account_id, cancelled);