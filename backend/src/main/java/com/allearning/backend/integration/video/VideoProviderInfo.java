package com.allearning.backend.integration.video;

/**
 * Metadata retrieved from the video provider's API.
 */
public record VideoProviderInfo(
        boolean exists,
        Integer durationSeconds,
        boolean encodingFinished
) {
}
