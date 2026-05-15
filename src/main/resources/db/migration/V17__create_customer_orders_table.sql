-- Pedidos de clientes do modo comprador.
-- Um pedido representa a demanda que origina um lote de compra.
-- Status é derivado: PENDING = sem lote, FULFILLED = lote vinculado.

CREATE TABLE customer_orders (
    id                UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id        UUID           NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    customer_name     VARCHAR(150)   NOT NULL,
    customer_phone    VARCHAR(20),
    customer_document VARCHAR(20),
    quantity_kg       DECIMAL(12, 4) NOT NULL,
    price_per_kg      DECIMAL(10, 4),
    product           VARCHAR(100)   NOT NULL,
    order_date        DATE           NOT NULL,
    delivery_deadline DATE,
    notes             TEXT,
    created_at        TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_customer_orders_account      ON customer_orders (account_id);
CREATE INDEX idx_customer_orders_account_date ON customer_orders (account_id, order_date DESC);
