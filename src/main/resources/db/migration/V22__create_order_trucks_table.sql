CREATE TABLE order_trucks (
    id            UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    leg_id        UUID           NOT NULL REFERENCES order_supplier_legs (id) ON DELETE CASCADE,
    truck_plate   VARCHAR(10)    NOT NULL,
    quantity_kg   DECIMAL(12, 2) NOT NULL,
    freight_value DECIMAL(12, 2),
    notes         TEXT,
    created_at    TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_order_trucks_leg ON order_trucks (leg_id);
