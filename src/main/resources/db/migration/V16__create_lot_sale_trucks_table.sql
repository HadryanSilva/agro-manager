-- Caminhões de entrega de um pedido de venda.
-- O comprador solicita X caminhões — cada um tem sua placa e peso registrados.

CREATE TABLE lot_sale_trucks (
    id          UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    sale_id     UUID           NOT NULL REFERENCES lot_sales (id) ON DELETE CASCADE,

    truck_plate VARCHAR(10)    NOT NULL,
    quantity_kg DECIMAL(12, 2) NOT NULL,
    notes       TEXT,

    created_at  TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_lot_sale_trucks_sale ON lot_sale_trucks (sale_id);
