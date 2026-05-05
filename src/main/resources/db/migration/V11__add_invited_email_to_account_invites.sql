-- Adiciona o e-mail do convidado à tabela de convites.
-- A partir desta migration, todos os convites são nominais:
-- apenas o usuário cujo e-mail bate com invited_email pode aceitar.
--
-- Convites existentes (antes da migration) ficam com invited_email NULL
-- e continuam funcionando sem restrição de e-mail.

ALTER TABLE account_invites
    ADD COLUMN invited_email VARCHAR(255);

-- Índice para detectar convite ativo duplicado para o mesmo e-mail na mesma conta
-- (verificado na camada de serviço, mas o índice acelera a query)
CREATE INDEX idx_account_invites_email
    ON account_invites (account_id, invited_email)
    WHERE used_at IS NULL;