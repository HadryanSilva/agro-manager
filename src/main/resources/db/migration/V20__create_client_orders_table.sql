CREATE TABLE client_orders (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID REFERENCES trading_clients (id) ON DELETE SET NULL
);
