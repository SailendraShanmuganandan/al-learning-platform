-- V14: Announcements and Contact Messages (FR-CON-01 to 02, BR-26, BR-27)

CREATE TABLE announcements (
    announcement_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_by BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    published_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_announcements_created_by FOREIGN KEY (created_by) REFERENCES users (user_id)
);

CREATE INDEX idx_announcements_status_published_at ON announcements (status, published_at DESC);

CREATE TABLE contact_messages (
    message_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL,
    phone VARCHAR(30) NULL,
    message TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'UNREAD' CHECK (status IN ('UNREAD', 'READ', 'RESPONDED', 'ARCHIVED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_contact_messages_status_created_at ON contact_messages (status, created_at DESC);
