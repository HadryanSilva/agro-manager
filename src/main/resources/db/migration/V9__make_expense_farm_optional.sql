-- Permite despesas não vinculadas a uma lavoura específica.
-- farm_id NULL indica despesa geral da conta.
-- Despesas existentes (farm_id NOT NULL) não são afetadas.

ALTER TABLE expenses
    ALTER COLUMN farm_id DROP NOT NULL;

-- Índice para listagem de despesas gerais da conta (farm_id IS NULL)
CREATE INDEX idx_expenses_account_general
    ON expenses (farm_id)
    WHERE farm_id IS NULL;