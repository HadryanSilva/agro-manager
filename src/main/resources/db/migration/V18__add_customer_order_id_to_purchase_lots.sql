-- Vincula cada lote de compra a um pedido de cliente.
-- NOT NULL + UNIQUE garante 1:1 no banco: um lote para exatamente um pedido.

ALTER TABLE purchase_lots
    ADD COLUMN customer_order_id UUID NOT NULL
        REFERENCES customer_orders(id);

ALTER TABLE purchase_lots
    ADD CONSTRAINT uq_purchase_lots_customer_order UNIQUE (customer_order_id);

CREATE INDEX idx_purchase_lots_customer_order ON purchase_lots (customer_order_id);
