-- V5: Subjects table (FR-ACAD-01)
-- Deactivation is the removal mechanism — no hard delete.
-- Case-insensitive name uniqueness enforced via functional index.

CREATE TABLE subjects (
    subject_id  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                             CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Fast per-status queries
CREATE INDEX idx_subjects_status ON subjects (status);

-- Enforce case-insensitive name uniqueness (FR-ACAD-01)
CREATE UNIQUE INDEX uq_subjects_name_lower ON subjects (lower(name));
