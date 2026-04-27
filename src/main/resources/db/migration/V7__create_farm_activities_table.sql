-- Histórico de atividades de uma lavoura.
-- Gerado automaticamente pelos serviços (despesas, farm) ou manualmente pelo usuário (NOTE).

CREATE TABLE farm_activities (
                                 id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                                 farm_id     UUID        NOT NULL REFERENCES farms (id) ON DELETE CASCADE,
                                 user_id     UUID        NOT NULL REFERENCES users (id),

    -- Tipo da atividade: EXPENSE_CREATED | EXPENSE_UPDATED | EXPENSE_DELETED |
    --                    EXPENSE_PAID    | FARM_UPDATED     | NOTE
                                 type        VARCHAR(30) NOT NULL,

    -- Descrição legível gerada automaticamente ou digitada pelo usuário
                                 description TEXT        NOT NULL,

    -- Referência opcional ao objeto relacionado (ex: id da despesa)
                                 related_id  UUID,

                                 created_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- Índice principal para listagem por lavoura cronológica
CREATE INDEX idx_farm_activities_farm ON farm_activities (farm_id, created_at DESC);