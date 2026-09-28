-- V4: Create addresses table (FR-USR-02 to FR-USR-04)
-- No ON DELETE CASCADE from users — address history is preserved independently.
-- A partial unique index enforces at most one default address per user.

CREATE TABLE addresses (
    address_id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id        BIGINT       NOT NULL REFERENCES users (user_id),
    recipient_name VARCHAR(150) NOT NULL,
    phone          VARCHAR(20)  NOT NULL,
    address_line_1 VARCHAR(255) NOT NULL,
    address_line_2 VARCHAR(255),
    city           VARCHAR(100) NOT NULL,
    district       VARCHAR(100) NOT NULL,
    postal_code    VARCHAR(20)  NOT NULL,
    is_default     BOOLEAN      NOT NULL DEFAULT false,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Index for fast per-user queries
CREATE INDEX idx_addresses_user_id ON addresses (user_id);

-- Enforce at most one default address per user (FR-USR-04)
CREATE UNIQUE INDEX uq_addresses_user_default
    ON addresses (user_id)
    WHERE is_default = true;
