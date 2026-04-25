-- Criação da tabela de convites de conta
-- Um OWNER ou ADMIN gera um convite com token único;
-- qualquer usuário autenticado que possua o token pode aceitar e se tornar membro.
--
-- used_at NULL     → convite ativo (aguardando aceite)
-- used_at NOT NULL → convite já utilizado

CREATE TABLE account_invites (
                                 id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                                 account_id  UUID        NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,

    -- Token único compartilhado na URL de convite
                                 token       UUID        NOT NULL UNIQUE,

    -- Papel a ser atribuído ao usuário ao aceitar
                                 role        VARCHAR(20) NOT NULL DEFAULT 'MEMBER',

    -- Quem gerou o convite
                                 created_by  UUID        NOT NULL REFERENCES users (id),

    -- Validade do convite (padrão: 7 dias após criação)
                                 expires_at  TIMESTAMP   NOT NULL,

    -- Preenchido quando o convite é aceito
                                 used_at     TIMESTAMP,

                                 created_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- Índice para busca rápida pelo token (operação mais frequente)
CREATE INDEX idx_account_invites_token   ON account_invites (token);

-- Índice para listar convites ativos de uma conta
CREATE INDEX idx_account_invites_account ON account_invites (account_id, used_at, expires_at);