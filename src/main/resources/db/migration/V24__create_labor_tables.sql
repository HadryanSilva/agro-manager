CREATE TABLE employees (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id  UUID NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
    name        VARCHAR(150) NOT NULL,
    daily_rate  DECIMAL(12, 2) NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    notes       TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_employees_daily_rate_positive CHECK (daily_rate > 0)
);

CREATE INDEX idx_employees_account_name ON employees (account_id, name);
CREATE INDEX idx_employees_account_active ON employees (account_id, active);

CREATE TABLE employee_payments (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id    UUID NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
    employee_id   UUID NOT NULL REFERENCES employees (id) ON DELETE RESTRICT,
    period_start  DATE NOT NULL,
    period_end    DATE NOT NULL,
    payment_date  DATE NOT NULL,
    total_amount  DECIMAL(12, 2) NOT NULL,
    notes         TEXT,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_employee_payments_period CHECK (period_end >= period_start),
    CONSTRAINT chk_employee_payments_total_positive CHECK (total_amount > 0)
);

CREATE INDEX idx_employee_payments_account_date ON employee_payments (account_id, payment_date DESC);
CREATE INDEX idx_employee_payments_employee_period ON employee_payments (employee_id, period_start, period_end);

CREATE TABLE employee_work_entries (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id    UUID NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
    employee_id   UUID NOT NULL REFERENCES employees (id) ON DELETE RESTRICT,
    farm_id       UUID REFERENCES farms (id) ON DELETE RESTRICT,
    work_date     DATE NOT NULL,
    daily_rate    DECIMAL(12, 2) NOT NULL,
    payment_id    UUID REFERENCES employee_payments (id) ON DELETE RESTRICT,
    notes         TEXT,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_employee_work_entries_employee_date UNIQUE (employee_id, work_date),
    CONSTRAINT chk_employee_work_entries_daily_rate_positive CHECK (daily_rate > 0)
);

CREATE INDEX idx_employee_work_entries_account_date ON employee_work_entries (account_id, work_date DESC);
CREATE INDEX idx_employee_work_entries_employee_date ON employee_work_entries (employee_id, work_date DESC);
CREATE INDEX idx_employee_work_entries_farm_date ON employee_work_entries (farm_id, work_date DESC);
CREATE INDEX idx_employee_work_entries_pending ON employee_work_entries (employee_id, work_date)
    WHERE payment_id IS NULL;

CREATE TABLE employee_payment_expenses (
    payment_id  UUID NOT NULL REFERENCES employee_payments (id) ON DELETE CASCADE,
    expense_id  UUID NOT NULL REFERENCES expenses (id) ON DELETE RESTRICT,

    PRIMARY KEY (payment_id, expense_id),
    CONSTRAINT uk_employee_payment_expenses_expense UNIQUE (expense_id)
);
