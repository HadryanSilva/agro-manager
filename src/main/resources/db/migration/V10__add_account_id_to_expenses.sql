-- Adiciona account_id à tabela expenses para garantir o vínculo correto de tenant
-- mesmo em despesas gerais (farm_id IS NULL).
-- Necessário pois a entidade Expense.java exige account_id como NOT NULL.

-- Adiciona a coluna como nullable primeiro para não quebrar dados existentes
ALTER TABLE expenses
    ADD COLUMN account_id UUID REFERENCES accounts (id) ON DELETE CASCADE;

-- Preenche account_id a partir do farm para despesas vinculadas a lavouras
UPDATE expenses e
SET account_id = f.account_id
    FROM farms f
WHERE e.farm_id = f.id
  AND e.account_id IS NULL;

-- Agora torna a coluna obrigatória
ALTER TABLE expenses
    ALTER COLUMN account_id SET NOT NULL;

-- Índice para buscas de despesas por conta (dashboard, transações gerais)
CREATE INDEX idx_expenses_account ON expenses (account_id);