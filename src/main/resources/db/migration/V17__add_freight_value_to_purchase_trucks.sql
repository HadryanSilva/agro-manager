-- Adds an optional freight cost per truck on purchase lots.
-- NULL means no freight was recorded (backwards-compatible with existing rows).
ALTER TABLE purchase_trucks
    ADD COLUMN freight_value DECIMAL(12, 2);
