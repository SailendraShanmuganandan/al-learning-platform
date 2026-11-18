-- V17: Add optional PDF attachment to video courses
ALTER TABLE videos
    ADD COLUMN pdf_file_path VARCHAR(500) NULL,
    ADD COLUMN pdf_original_filename VARCHAR(255) NULL,
    ADD COLUMN pdf_file_size BIGINT NULL;
