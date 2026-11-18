package com.allearning.backend.integration.video;

import java.time.Duration;
import java.util.Optional;

/**
 * Service contract for protected video playback provider operations.
 * Isolates all provider-specific URL signing and API calls behind this abstraction.
 */
public interface VideoPlaybackProvider {

    /**
     * Checks if the required playback credentials (library ID and token security key) are configured.
     */
    boolean isConfigured();

    /**
     * Generates a signed embed player URL for the given provider video ID with the specified time-to-live.
     *
     * @param providerVideoId provider-assigned video identifier
     * @param ttl             duration for which the signed embed player remains valid
     * @return signed embed result with the full embed URL and expiration timestamp
     */
    SignedEmbedResult signEmbedUrl(String providerVideoId, Duration ttl);

    /**
     * Overload for convenience specifying TTL in minutes.
     */
    default SignedEmbedResult signEmbedUrl(String providerVideoId, int ttlMinutes) {
        return signEmbedUrl(providerVideoId, Duration.ofMinutes(ttlMinutes));
    }

    /**
     * Optional provider metadata query (requires API key).
     *
     * @param providerVideoId provider-assigned video identifier
     * @return provider video info if API key is configured and response received; empty if unconfigured or call failed
     */
    Optional<VideoProviderInfo> getVideoInfo(String providerVideoId);
}
