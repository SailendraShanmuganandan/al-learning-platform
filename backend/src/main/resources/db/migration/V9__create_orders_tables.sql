-- V9: Orders and order items tables (FR-ORDER-01 to FR-ORDER-10, BR-31, BR-32)
-- Commerce checkout and order management.

CREATE TABLE orders (
    order_id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES users (user_id),
    ship_recipient_name VARCHAR(150),
    ship_phone          VARCHAR(20),
    ship_address_line_1 VARCHAR(255),
    ship_address_line_2 VARCHAR(255),
    ship_city           VARCHAR(100),
    ship_district       VARCHAR(100),
    ship_postal_code    VARCHAR(20),
    payment_status      VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                            CHECK (payment_status IN ('PENDING', 'PAID', 'FAILED', 'REFUNDED', 'EXPIRED')),
    fulfillment_status  VARCHAR(20) NOT NULL
                            CHECK (fulfillment_status IN ('NOT_REQUIRED', 'PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED')),
    subtotal            NUMERIC(12, 2) NOT NULL CHECK (subtotal >= 0),
    delivery_fee        NUMERIC(12, 2) NOT NULL CHECK (delivery_fee >= 0),
    total_amount        NUMERIC(12, 2) NOT NULL CHECK (total_amount >= 0),
    expires_at          TIMESTAMPTZ NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Index for the order expiry scheduler job (payment_status = 'PENDING' and expires_at < now)
CREATE INDEX idx_orders_payment_expires ON orders (payment_status, expires_at);

-- Index for user order history queries (newest first)
CREATE INDEX idx_orders_user_created ON orders (user_id, created_at DESC);

CREATE TABLE order_items (
    order_item_id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id        BIGINT NOT NULL REFERENCES orders (order_id),
    product_id      BIGINT NOT NULL REFERENCES products (product_id),
    product_name    VARCHAR(300) NOT NULL,
    product_type    VARCHAR(30) NOT NULL,
    quantity        INTEGER NOT NULL CHECK (quantity > 0),
    unit_price      NUMERIC(12, 2) NOT NULL CHECK (unit_price >= 0),
    subtotal        NUMERIC(12, 2) NOT NULL CHECK (subtotal >= 0),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);
CREATE INDEX idx_order_items_product_id ON order_items (product_id);
