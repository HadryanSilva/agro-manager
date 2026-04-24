-- Criação da tabela de membros de conta
-- Relaciona usuários a contas com um papel (OWNER, ADMIN, MEMBER)
-- Um usuário pode pertencer a múltiplas contas

CREATE TABLE account_members (
                                 id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                                 account_id  UUID        NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
                                 user_id     UUID        NOT NULL REFERENCES users (id)    ON DELETE CASCADE,
                                 role        VARCHAR(20) NOT NULL,
                                 joined_at   TIMESTAMP   NOT NULL DEFAULT NOW(),

    -- Garante que um usuário aparece apenas uma vez por conta
                                 CONSTRAINT uq_account_members UNIQUE (account_id, user_id)
);

-- Índice para buscar todas as contas de um usuário (query mais frequente)
CREATE INDEX idx_account_members_user ON account_members (user_id);

-- Índice para buscar todos os membros de uma conta
CREATE INDEX idx_account_members_account ON account_members (account_id);