package com.allearning.backend.integration.video;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BunnyVideoPlaybackProviderTest {

    private static final Pattern EMBED_URL_PATTERN = Pattern.compile(
            "^https://iframe\\.mediadelivery\\.net/embed/([^/]+)/([^/?]+)\\?token=([0-9a-f]{64})&expires=(\\d+)$"
    );

    @Test
    @DisplayName("Unit test: computeToken matches independently computed SHA-256 hex value")
    void computeToken_matchesIndependentlyComputedSha256() {
        String tokenKey = "test-token-key-12345";
        String videoId = "32d140e2-e4f4-4eec-9d53-20371e9be607";
        long expires = 1700000000L;

        String token = BunnyVideoPlaybackProvider.computeToken(tokenKey, videoId, expires);

        // Pre-computed independently via SHA-256 of "test-token-key-1234532d140e2-e4f4-4eec-9d53-20371e9be6071700000000"
        assertThat(token).isEqualTo("0122352744842d73678df6dae2bdfe3c2e1576cab5310ccf6fcf31f9eefe5a25");
    }

    @Test
    @DisplayName("Unit test: computeToken matches official Bunny documentation example")
    void computeToken_matchesOfficialBunnyDocsExample() {
        String tokenKey = "4742a81b-bf15-42fe-8b1c-8fcb9024c550";
        String videoId = "32d140e2-e4f4-4eec-9d53-20371e9be607";
        long expires = 1623440202L;

        String token = BunnyVideoPlaybackProvider.computeToken(tokenKey, videoId, expires);

        assertThat(token).isEqualTo("a8617f6df2e9b55b65ac7112138c70417766d80614bfe146d0d9bb2bd21fef87");
    }

    @Test
    @DisplayName("Unit test: signEmbedUrl produces documented URL shape, 64-char hex token, and future expiry")
    void signEmbedUrl_producesDocumentedUrlShapeAndFutureExpiry() {
        String libraryId = "12345";
        String tokenKey = "my-secret-token-key";
        String videoId = "98765432-abcd-ef01-2345-6789abcdef01";

        BunnyVideoPlaybackProvider provider = new BunnyVideoPlaybackProvider(
                libraryId,
                tokenKey,
                null,
                120
        );

        assertThat(provider.isConfigured()).isTrue();

        Instant before = Instant.now();
        SignedEmbedResult result = provider.signEmbedUrl(videoId, Duration.ofMinutes(120));
        Instant after = Instant.now();

        assertThat(result).isNotNull();
        assertThat(result.embedUrl()).isNotBlank();
        assertThat(result.expiresAt()).isNotNull();

        // Check URL matches https://iframe.mediadelivery.net/embed/{libraryId}/{videoId}?token=...&expires=...
        Matcher matcher = EMBED_URL_PATTERN.matcher(result.embedUrl());
        assertThat(matcher.matches()).as("Embed URL must match documented shape").isTrue();

        assertThat(matcher.group(1)).isEqualTo(libraryId);
        assertThat(matcher.group(2)).isEqualTo(videoId);

        String tokenInUrl = matcher.group(3);
        assertThat(tokenInUrl).hasSize(64);

        long expiresEpochSeconds = Long.parseLong(matcher.group(4));
        assertThat(result.expiresAt().toEpochSecond()).isEqualTo(expiresEpochSeconds);

        // Assert future expiry roughly 120 minutes ahead
        long expectedMinExpiry = before.getEpochSecond() + 120 * 60;
        long expectedMaxExpiry = after.getEpochSecond() + 120 * 60;
        assertThat(expiresEpochSeconds).isBetween(expectedMinExpiry, expectedMaxExpiry);
    }

    @Test
    @DisplayName("Unit test: signEmbedUrl throws 503 VIDEO_PLAYBACK_NOT_CONFIGURED when credentials are unset")
    void signEmbedUrl_unconfiguredThrows503() {
        BunnyVideoPlaybackProvider unconfigured = new BunnyVideoPlaybackProvider(
                "",
                "",
                null,
                120
        );

        assertThat(unconfigured.isConfigured()).isFalse();

        assertThatThrownBy(() -> unconfigured.signEmbedUrl("32d140e2-e4f4-4eec-9d53-20371e9be607", 120))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(503);
                    assertThat(rse.getReason()).contains("VIDEO_PLAYBACK_NOT_CONFIGURED");
                });
    }
}
