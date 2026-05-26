-- CHAR(8) (bpchar) conflicts with Hibernate's VARCHAR mapping for String fields.
-- Alter to VARCHAR(8) so schema validation passes without columnDefinition overrides.
ALTER TABLE account_invites ALTER COLUMN code TYPE VARCHAR(8);
