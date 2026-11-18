package com.allearning.backend.catalog.dto;

import java.time.OffsetDateTime;

/**
 * Response containing a signed embed player URL for admin preview playback.
 */
public record AdminPlaybackResponse(
        String embedUrl,
        OffsetDateTime expiresAt
) {
}
