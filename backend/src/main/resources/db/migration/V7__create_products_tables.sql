-- V7: Products, Notes, Videos, and Product Chapters (FR-PROD-01 to FR-PROD-11)
-- Catalog domain data model for physical notes and on-demand video courses.

CREATE TABLE products (
    product_id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name            VARCHAR(300)   NOT NULL,
    slug            VARCHAR(350)   NOT NULL UNIQUE,
    description     TEXT,
    product_type    VARCHAR(30)    NOT NULL
                                   CHECK (product_type IN ('PHYSICAL_NOTE', 'VIDEO')),
    price           NUMERIC(12, 2) NOT NULL
                                   CHECK (price >= 0),
    status          VARCHAR(20)    NOT NULL DEFAULT 'DRAFT'
                                   CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    thumbnail_url   VARCHAR(1000),
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT now()
);

-- Subtype table for PHYSICAL_NOTE products
CREATE TABLE notes (
    product_id      BIGINT PRIMARY KEY REFERENCES products (product_id),
    page_count      INTEGER NOT NULL CHECK (page_count > 0),
    stock_quantity  INTEGER NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0)
);

-- Subtype table for VIDEO products
CREATE TABLE videos (
    product_id       BIGINT PRIMARY KEY REFERENCES products (product_id),
    video_url        VARCHAR(1000) NOT NULL,
    duration_seconds INTEGER       NOT NULL CHECK (duration_seconds > 0),
    description      TEXT
);

-- Many-to-many junction table between products and chapters (composite primary key)
CREATE TABLE product_chapters (
    product_id  BIGINT NOT NULL REFERENCES products (product_id),
    chapter_id  BIGINT NOT NULL REFERENCES chapters (chapter_id),
    PRIMARY KEY (product_id, chapter_id)
);

-- Indexes for efficient queries
CREATE INDEX idx_products_type ON products (product_type);
CREATE INDEX idx_products_status ON products (status);
CREATE INDEX idx_products_name ON products (name);
CREATE INDEX idx_product_chapters_chapter_id ON product_chapters (chapter_id);
