-- Adds short invite code for manual entry on onboarding screen.
-- Code is stored as 8 uppercase alphanumeric chars (no hyphen).
-- gen_random_uuid() is VOLATILE in PostgreSQL — called once per row.

ALTER TABLE account_invites ADD COLUMN code CHAR(8);

UPDATE account_invites
SET code = upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 8));

ALTER TABLE account_invites ALTER COLUMN code SET NOT NULL;

CREATE UNIQUE INDEX idx_account_invites_code ON account_invites (code);
