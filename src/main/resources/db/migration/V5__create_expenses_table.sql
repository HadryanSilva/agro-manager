-- Criação da tabela de despesas por lavoura
-- Suporta dois tipos: INSUMO (sementes, fertilizantes, defensivos...)
--                    SERVICO (máquinas, mão de obra, frete...)
-- O status de pagamento é derivado da presença/ausência de payment_date:
--   payment_date NULL    → a pagar
--   payment_date NOT NULL → pago

CREATE TABLE expenses (
                          id               UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
                          farm_id          UUID           NOT NULL REFERENCES farms (id) ON DELETE CASCADE,

                          description      VARCHAR(200)   NOT NULL,
                          category         VARCHAR(20)    NOT NULL,
                          value            DECIMAL(12, 2) NOT NULL,

    -- Data de competência: quando a despesa foi incorrida
                          competence_date  DATE           NOT NULL,

    -- Data de pagamento: null = a pagar, preenchida = pago
                          payment_date     DATE,

                          notes            TEXT,

                          created_at       TIMESTAMP      NOT NULL DEFAULT NOW(),
                          updated_at       TIMESTAMP      NOT NULL DEFAULT NOW()
);

-- Índice principal para listagem de despesas por lavoura
CREATE INDEX idx_expenses_farm             ON expenses (farm_id);

-- Índice para futuras consultas filtradas por categoria
CREATE INDEX idx_expenses_farm_category    ON expenses (farm_id, category);

-- Índice para consultas de inadimplência (payment_date IS NULL)
CREATE INDEX idx_expenses_farm_payment     ON expenses (farm_id, payment_date);