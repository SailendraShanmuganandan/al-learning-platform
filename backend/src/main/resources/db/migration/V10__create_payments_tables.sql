-- V10: Payments and payment notifications tables (FR-PAY-01 to FR-PAY-09, BR-13, SEC-06, SEC-09)
-- Integration with PayHere Redirect Checkout.

CREATE TABLE payments (
    payment_id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id               BIGINT NOT NULL REFERENCES orders (order_id),
    payment_method         VARCHAR(50) NOT NULL DEFAULT 'PAYHERE',
    transaction_reference  VARCHAR(255) NULL,
    amount                 NUMERIC(12, 2) NOT NULL,
    payment_status         VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                               CHECK (payment_status IN ('PENDING', 'PAID', 'FAILED', 'REFUNDED')),
    paid_at                TIMESTAMPTZ NULL,
    needs_review           BOOLEAN NOT NULL DEFAULT false,
    review_reason          VARCHAR(255) NULL,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Partial unique index on transaction_reference (FR-PAY-04)
CREATE UNIQUE INDEX uq_payments_transaction_reference
    ON payments (transaction_reference)
    WHERE transaction_reference IS NOT NULL;

-- Index on payments (order_id)
CREATE INDEX idx_payments_order_id ON payments (order_id);

-- Payment notifications table for audit logging
CREATE TABLE payment_notifications (
    notification_id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id           BIGINT NULL,
    payhere_payment_id VARCHAR(255) NULL,
    status_code        INTEGER NULL,
    amount             NUMERIC(12, 2) NULL,
    currency           VARCHAR(10) NULL,
    signature_valid    BOOLEAN NOT NULL,
    outcome            VARCHAR(100) NOT NULL,
    received_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_payment_notifications_order_id ON payment_notifications (order_id);
