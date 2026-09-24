-- V1: Create users table
-- Role and account_status are stored as VARCHAR with CHECK constraints
-- so they are easy to evolve without PostgreSQL ALTER TYPE operations.
-- Email uniqueness is enforced on the lower-cased value for case-insensitive lookups.

CREATE TABLE users (
    user_id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    full_name      VARCHAR(150)                NOT NULL,
    email          VARCHAR(255)                NOT NULL,
    password_hash  VARCHAR(255)                NOT NULL,
    phone          VARCHAR(20),
    role           VARCHAR(20)                 NOT NULL DEFAULT 'STUDENT'
                       CONSTRAINT chk_users_role CHECK (role IN ('STUDENT', 'ADMIN')),
    account_status VARCHAR(20)                 NOT NULL DEFAULT 'ACTIVE'
                       CONSTRAINT chk_users_account_status CHECK (account_status IN ('ACTIVE', 'SUSPENDED', 'DELETED')),
    created_at     TIMESTAMPTZ                 NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ                 NOT NULL DEFAULT now()
);

-- Case-insensitive unique index on email
CREATE UNIQUE INDEX uq_users_email_lower ON users (lower(email));
