package com.allearning.backend.integration.video;

import java.time.OffsetDateTime;

/**
 * Result of signing an embed player URL with a bounded expiration.
 */
public record SignedEmbedResult(
        String embedUrl,
        OffsetDateTime expiresAt
) {
}
