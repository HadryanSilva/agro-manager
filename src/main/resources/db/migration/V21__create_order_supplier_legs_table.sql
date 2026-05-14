CREATE TABLE order_supplier_legs (
    id                     UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id               UUID           NOT NULL REFERENCES client_orders (id) ON DELETE CASCADE,
    supplier_id            UUID           NOT NULL REFERENCES trading_suppliers (id),
    supplier_price_per_kg  DECIMAL(12, 2) NOT NULL,
    notes                  TEXT,
    created_at             TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_order_supplier_legs_order ON order_supplier_legs (order_id);
