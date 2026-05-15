-- Store local and OAuth e-mails canonically and enforce case-insensitive uniqueness.
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_email_key;

UPDATE users
SET email = LOWER(TRIM(email));

CREATE UNIQUE INDEX IF NOT EXISTS ux_users_email_lower ON users (LOWER(email));
