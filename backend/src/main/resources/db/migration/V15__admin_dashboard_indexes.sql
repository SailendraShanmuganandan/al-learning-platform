-- V15: Indexes for Admin Dashboard queries
-- Optimizes sales, fulfillment and recent activity aggregate lookups.

CREATE INDEX IF NOT EXISTS idx_payments_status_paid_at
    ON payments (payment_status, paid_at);

CREATE INDEX IF NOT EXISTS idx_orders_status_fulfillment
    ON orders (payment_status, fulfillment_status);

CREATE INDEX IF NOT EXISTS idx_orders_created_at
    ON orders (created_at DESC);
