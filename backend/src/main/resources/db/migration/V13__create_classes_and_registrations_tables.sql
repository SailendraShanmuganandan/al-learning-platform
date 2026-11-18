-- V13: Classes and Class Registrations (FR-CLASS-01 to 08, BR-25)

CREATE TABLE classes (
    class_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    subject_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    class_type VARCHAR(50) NOT NULL CHECK (class_type IN ('PHYSICAL', 'ONLINE', 'HYBRID')),
    schedule_description VARCHAR(500) NOT NULL,
    fee NUMERIC(12, 2) NOT NULL CHECK (fee >= 0),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_classes_subject FOREIGN KEY (subject_id) REFERENCES subjects (subject_id)
);

CREATE TABLE class_registrations (
    registration_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'REJECTED')),
    registered_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_registrations_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_registrations_class FOREIGN KEY (class_id) REFERENCES classes (class_id)
);

-- Partial unique index: at most one active (PENDING or CONFIRMED) registration per user per class (FR-CLASS-07)
CREATE UNIQUE INDEX idx_class_registrations_active_user_class
    ON class_registrations (user_id, class_id)
    WHERE status IN ('PENDING', 'CONFIRMED');

CREATE INDEX idx_classes_subject_id ON classes (subject_id);
CREATE INDEX idx_classes_status ON classes (status);
CREATE INDEX idx_class_registrations_class_id ON class_registrations (class_id);
CREATE INDEX idx_class_registrations_user_id ON class_registrations (user_id);
CREATE INDEX idx_class_registrations_status ON class_registrations (status);
