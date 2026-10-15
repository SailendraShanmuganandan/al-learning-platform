-- V8: Shopping cart tables (FR-CART-01 to FR-CART-09, BR-05)
-- Carts and cart items for student shopping cart slice.

CREATE TABLE carts (
    cart_id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users (user_id),
    status      VARCHAR(30) NOT NULL DEFAULT 'ACTIVE'
                    CHECK (status IN ('ACTIVE', 'CONVERTED', 'ABANDONED')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- BR-05: One active cart per user enforced via partial unique index
CREATE UNIQUE INDEX idx_carts_user_active ON carts (user_id) WHERE status = 'ACTIVE';

CREATE TABLE cart_items (
    cart_item_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cart_id      BIGINT NOT NULL REFERENCES carts (cart_id) ON DELETE CASCADE,
    product_id   BIGINT NOT NULL REFERENCES products (product_id),
    quantity     INTEGER NOT NULL CHECK (quantity > 0),
    unit_price   NUMERIC(12, 2) NOT NULL CHECK (unit_price >= 0),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_cart_items_cart_product UNIQUE (cart_id, product_id)
);

CREATE INDEX idx_cart_items_cart_id ON cart_items (cart_id);
CREATE INDEX idx_cart_items_product_id ON cart_items (product_id);
