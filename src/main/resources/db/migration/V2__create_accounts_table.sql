-- Criação da tabela de contas (entidade central da aplicação)
-- Toda funcionalidade do sistema estará vinculada a uma Account

CREATE TABLE accounts (
                          id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          name        VARCHAR(255) NOT NULL,
                          owner_id    UUID         NOT NULL REFERENCES users (id),
                          created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
                          updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Índice para buscas de contas pelo dono
CREATE INDEX idx_accounts_owner ON accounts (owner_id);