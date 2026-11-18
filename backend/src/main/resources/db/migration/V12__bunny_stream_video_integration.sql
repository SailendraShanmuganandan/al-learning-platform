-- V12: Bunny Stream video integration (FR-VID-01, FR-VID-05, FR-VID-06, BR-10)
-- Replaces legacy direct video URLs with Bunny Stream provider_video_id and token authentication.

ALTER TABLE videos
    ADD COLUMN video_provider VARCHAR(50) NOT NULL DEFAULT 'BUNNY',
    ADD COLUMN provider_video_id VARCHAR(100) NULL,
    ALTER COLUMN video_url DROP NOT NULL;

-- Partial unique index: one provider video belongs to at most one product
CREATE UNIQUE INDEX idx_videos_provider_video_id
    ON videos (provider_video_id)
    WHERE provider_video_id IS NOT NULL;
