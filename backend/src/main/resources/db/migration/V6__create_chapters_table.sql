-- V6: Chapters table (FR-ACAD-02)
-- Chapters belong to a subject. Deactivation is the removal mechanism — no hard delete.
-- The unique constraint on (subject_id, chapter_order) applies regardless of chapter status,
-- so an inactive chapter still holds its position.

CREATE TABLE chapters (
    chapter_id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    subject_id      BIGINT       NOT NULL
                                 REFERENCES subjects (subject_id),
    name            VARCHAR(300) NOT NULL,
    chapter_order   INTEGER      NOT NULL
                                 CHECK (chapter_order > 0),
    description     TEXT,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                                 CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    -- An order number is unique within a subject, regardless of status.
    CONSTRAINT uq_chapters_subject_order UNIQUE (subject_id, chapter_order)
);

-- Fast lookup of chapters by subject, ordered for public and admin list views.
CREATE INDEX idx_chapters_subject_id ON chapters (subject_id, chapter_order);

-- Fast per-status queries within a subject.
CREATE INDEX idx_chapters_subject_status ON chapters (subject_id, status);
