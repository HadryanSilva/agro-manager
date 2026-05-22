ALTER TABLE expenses
  ADD COLUMN is_credit BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN due_date  DATE;

CREATE INDEX idx_expenses_upcoming
  ON expenses (account_id, due_date)
  WHERE is_credit = TRUE AND payment_date IS NULL;
