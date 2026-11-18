-- V16: Learning domain tables (quizzes, chapter topics, questions, options,
-- study sessions, attempts, diagnostic assessments, mistake records, question reports)

CREATE TABLE quizzes (
    quiz_id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    chapter_id  BIGINT NOT NULL UNIQUE REFERENCES chapters(chapter_id),
    title       VARCHAR(255) NOT NULL,
    description TEXT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE chapter_topics (
    topic_id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    chapter_id  BIGINT NOT NULL REFERENCES chapters(chapter_id),
    name        VARCHAR(255) NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_chapter_topics_chapter_name_lower
    ON chapter_topics (chapter_id, lower(name));

CREATE TABLE chapter_documents (
    document_id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    chapter_id         BIGINT NOT NULL REFERENCES chapters(chapter_id),
    file_name          VARCHAR(255) NOT NULL,
    file_type          VARCHAR(100) NOT NULL,
    file_size_bytes    BIGINT NOT NULL,
    storage_key        VARCHAR(500) NOT NULL,
    extracted_text     TEXT NULL,
    extraction_status  VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                       CHECK (extraction_status IN ('PENDING', 'EXTRACTED', 'NO_TEXT', 'FAILED')),
    is_chatbot_source  BOOLEAN NOT NULL DEFAULT false,
    status             VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                       CHECK (status IN ('ACTIVE', 'REMOVED')),
    uploaded_by        BIGINT NOT NULL REFERENCES users(user_id),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE question_generation_jobs (
    job_id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    chapter_id       BIGINT NOT NULL REFERENCES chapters(chapter_id),
    document_id      BIGINT NOT NULL REFERENCES chapter_documents(document_id),
    requested_count  INT NOT NULL,
    generated_count  INT NOT NULL DEFAULT 0,
    rejected_count   INT NOT NULL DEFAULT 0,
    language         VARCHAR(5) NOT NULL,
    status           VARCHAR(20) NOT NULL
                     CHECK (status IN ('PENDING', 'RUNNING', 'DONE', 'FAILED')),
    error_message    TEXT NULL,
    created_by       BIGINT NOT NULL REFERENCES users(user_id),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    started_at       TIMESTAMPTZ NULL,
    completed_at     TIMESTAMPTZ NULL
);

CREATE UNIQUE INDEX uq_jobs_chapter_pending_running
    ON question_generation_jobs (chapter_id)
    WHERE status IN ('PENDING', 'RUNNING');

CREATE TABLE questions (
    question_id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    quiz_id             BIGINT NOT NULL REFERENCES quizzes(quiz_id),
    question_text       TEXT NOT NULL,
    explanation         TEXT NULL,
    topic_id            BIGINT NULL REFERENCES chapter_topics(topic_id),
    difficulty          VARCHAR(20) NOT NULL
                        CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    language            VARCHAR(5) NOT NULL DEFAULT 'EN',
    status              VARCHAR(20) NOT NULL
                        CHECK (status IN ('PENDING', 'APPROVED', 'ARCHIVED')),
    source              VARCHAR(20) NOT NULL
                        CHECK (source IN ('MANUAL', 'AI')),
    source_document_id  BIGINT NULL REFERENCES chapter_documents(document_id),
    generation_job_id   BIGINT NULL REFERENCES question_generation_jobs(job_id),
    source_excerpt      TEXT NULL,
    created_by          BIGINT NOT NULL REFERENCES users(user_id),
    approved_by         BIGINT NULL REFERENCES users(user_id),
    approved_at         TIMESTAMPTZ NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_questions_quiz_status
    ON questions (quiz_id, status);

CREATE TABLE question_options (
    option_id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    question_id  BIGINT NOT NULL REFERENCES questions(question_id),
    option_text  TEXT NOT NULL,
    option_order INT NOT NULL,
    is_correct   BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT uq_question_options_order UNIQUE (question_id, option_order)
);

CREATE UNIQUE INDEX uq_question_options_single_correct
    ON question_options (question_id)
    WHERE is_correct = true;

CREATE TABLE study_sessions (
    session_id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id           BIGINT NOT NULL REFERENCES users(user_id),
    duration_minutes  INT NOT NULL
                      CHECK (duration_minutes IN (10, 20, 30, 60)),
    status            VARCHAR(20) NOT NULL
                      CHECK (status IN ('ACTIVE', 'COMPLETED', 'ABANDONED')),
    started_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at      TIMESTAMPTZ NULL
);

CREATE TABLE study_session_items (
    item_id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    session_id    BIGINT NOT NULL REFERENCES study_sessions(session_id),
    item_type     VARCHAR(30) NOT NULL
                  CHECK (item_type IN ('MISTAKE_REVIEW', 'PRACTICE', 'RESOURCE_REVIEW')),
    reference_id  BIGINT NULL,
    item_order    INT NOT NULL,
    completed_at  TIMESTAMPTZ NULL,
    CONSTRAINT uq_session_items_order UNIQUE (session_id, item_order)
);

CREATE TABLE quiz_attempts (
    attempt_id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id           BIGINT NOT NULL REFERENCES users(user_id),
    attempt_type      VARCHAR(30) NOT NULL
                      CHECK (attempt_type IN ('CHAPTER_QUIZ', 'MISTAKE_PRACTICE', 'STUDY_SESSION')),
    quiz_id           BIGINT NULL REFERENCES quizzes(quiz_id),
    study_session_id  BIGINT NULL REFERENCES study_sessions(session_id),
    score             INT NULL,
    total_marks       INT NOT NULL,
    started_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at      TIMESTAMPTZ NULL,
    CONSTRAINT chk_attempt_quiz_presence
        CHECK (
            (attempt_type = 'CHAPTER_QUIZ' AND quiz_id IS NOT NULL) OR
            (attempt_type IN ('MISTAKE_PRACTICE', 'STUDY_SESSION') AND quiz_id IS NULL)
        ),
    CONSTRAINT chk_attempt_session_presence
        CHECK (study_session_id IS NULL OR attempt_type = 'STUDY_SESSION')
);

CREATE UNIQUE INDEX uq_quiz_attempts_active_chapter_quiz
    ON quiz_attempts (user_id, quiz_id)
    WHERE completed_at IS NULL AND attempt_type = 'CHAPTER_QUIZ';

CREATE INDEX idx_quiz_attempts_user_completed
    ON quiz_attempts (user_id, completed_at);

CREATE TABLE quiz_answers (
    answer_id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    attempt_id          BIGINT NOT NULL REFERENCES quiz_attempts(attempt_id),
    question_id         BIGINT NOT NULL REFERENCES questions(question_id),
    question_order      INT NOT NULL,
    selected_option_id  BIGINT NULL REFERENCES question_options(option_id),
    is_correct          BOOLEAN NULL,
    CONSTRAINT uq_quiz_answers_question UNIQUE (attempt_id, question_id),
    CONSTRAINT uq_quiz_answers_order UNIQUE (attempt_id, question_order),
    CONSTRAINT chk_quiz_answers_null_consistency
        CHECK (
            (selected_option_id IS NULL AND is_correct IS NULL) OR
            (selected_option_id IS NOT NULL AND is_correct IS NOT NULL)
        )
);

CREATE INDEX idx_quiz_answers_attempt_id ON quiz_answers (attempt_id);
CREATE INDEX idx_quiz_answers_question_id ON quiz_answers (question_id);

CREATE TABLE diagnostic_assessments (
    assessment_id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id          BIGINT NOT NULL REFERENCES users(user_id),
    assessment_type  VARCHAR(30) NOT NULL
                     CHECK (assessment_type IN ('INITIAL_DIAGNOSTIC', 'OVERALL_QUIZ')),
    scope_type       VARCHAR(30) NOT NULL
                     CHECK (scope_type IN ('CHAPTERS', 'SUBJECT', 'ALL_SUBJECTS')),
    scope_reference  JSONB NULL,
    status           VARCHAR(20) NOT NULL
                     CHECK (status IN ('IN_PROGRESS', 'COMPLETED')),
    score            INT NULL,
    total_marks      INT NOT NULL,
    started_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at     TIMESTAMPTZ NULL
);

CREATE TABLE diagnostic_assessment_answers (
    answer_id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    assessment_id       BIGINT NOT NULL REFERENCES diagnostic_assessments(assessment_id),
    question_id         BIGINT NOT NULL REFERENCES questions(question_id),
    question_order      INT NOT NULL,
    selected_option_id  BIGINT NULL REFERENCES question_options(option_id),
    is_correct          BOOLEAN NULL,
    CONSTRAINT uq_diag_answers_question UNIQUE (assessment_id, question_id),
    CONSTRAINT uq_diag_answers_order UNIQUE (assessment_id, question_order),
    CONSTRAINT chk_diag_answers_null_consistency
        CHECK (
            (selected_option_id IS NULL AND is_correct IS NULL) OR
            (selected_option_id IS NOT NULL AND is_correct IS NOT NULL)
        )
);

CREATE INDEX idx_diag_answers_question_id ON diagnostic_assessment_answers (question_id);

CREATE TABLE mistake_records (
    mistake_id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id           BIGINT NOT NULL REFERENCES users(user_id),
    question_id       BIGINT NOT NULL REFERENCES questions(question_id),
    status            VARCHAR(20) NOT NULL
                      CHECK (status IN ('ACTIVE', 'MASTERED')),
    correct_streak    INT NOT NULL DEFAULT 0,
    last_answered_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    mastered_at       TIMESTAMPTZ NULL,
    CONSTRAINT uq_mistake_records_user_question UNIQUE (user_id, question_id)
);

CREATE INDEX idx_mistake_records_user_status ON mistake_records (user_id, status);

CREATE TABLE question_reports (
    report_id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    question_id  BIGINT NOT NULL REFERENCES questions(question_id),
    user_id      BIGINT NOT NULL REFERENCES users(user_id),
    reason       VARCHAR(30) NOT NULL
                 CHECK (reason IN ('WRONG_ANSWER', 'UNCLEAR', 'OTHER')),
    comment      TEXT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'OPEN'
                 CHECK (status IN ('OPEN', 'RESOLVED', 'DISMISSED')),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_by  BIGINT NULL REFERENCES users(user_id),
    resolved_at  TIMESTAMPTZ NULL,
    CONSTRAINT uq_question_reports_question_user UNIQUE (question_id, user_id)
);

