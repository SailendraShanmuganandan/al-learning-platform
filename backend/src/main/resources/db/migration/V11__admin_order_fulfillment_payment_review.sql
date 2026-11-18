-- V11: Admin order fulfillment and payment review support
-- Adds tracking_note to orders; adds review/refund audit fields to payments.

-- Orders: manual courier/tracking text for admin use
ALTER TABLE orders
    ADD COLUMN tracking_note VARCHAR(500) NULL;

-- Payments: admin review and refund audit columns
ALTER TABLE payments
    ADD COLUMN review_note   VARCHAR(1000) NULL,
    ADD COLUMN reviewed_by   BIGINT NULL,          -- admin user_id; no FK constraint required
    ADD COLUMN reviewed_at   TIMESTAMPTZ NULL,
    ADD COLUMN refunded_at   TIMESTAMPTZ NULL;

-- Index to speed up the needs_review badge count query
CREATE INDEX idx_payments_needs_review ON payments (needs_review)
    WHERE needs_review = true;
