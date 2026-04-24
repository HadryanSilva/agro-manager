-- Criação da tabela de usuários
-- Suporta autenticação local (e-mail/senha) e OAuth2 (Google)

CREATE TABLE users (
                       id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       name             VARCHAR(255)        NOT NULL,
                       email            VARCHAR(255)        NOT NULL UNIQUE,
                       password_hash    VARCHAR(255),
                       auth_provider    VARCHAR(20)         NOT NULL,
                       provider_id      VARCHAR(255),
                       avatar_url       VARCHAR(500),
                       email_verified   BOOLEAN             NOT NULL DEFAULT FALSE,
                       created_at       TIMESTAMP           NOT NULL DEFAULT NOW(),
                       updated_at       TIMESTAMP           NOT NULL DEFAULT NOW()
);

-- Índice para buscas por e-mail (login)
CREATE INDEX idx_users_email ON users (email);

-- Índice para buscas por provedor OAuth2
CREATE INDEX idx_users_provider ON users (provider_id, auth_provider);