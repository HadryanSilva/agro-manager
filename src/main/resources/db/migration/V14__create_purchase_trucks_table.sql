-- Caminhões que compõem um lote de compra.
-- Um lote pode chegar em múltiplos caminhões, cada um com sua placa e peso.

CREATE TABLE purchase_trucks (
    id          UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    lot_id      UUID           NOT NULL REFERENCES purchase_lots (id) ON DELETE CASCADE,

    truck_plate VARCHAR(10)    NOT NULL,
    quantity_kg DECIMAL(12, 2) NOT NULL,
    notes       TEXT,

    created_at  TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_purchase_trucks_lot ON purchase_trucks (lot_id);
