package com.allearning.backend.integration.video;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Bunny Stream implementation of {@link VideoPlaybackProvider}.
 * Handles token generation for iframe embed view token authentication and metadata querying.
 */
@Component
public class BunnyVideoPlaybackProvider implements VideoPlaybackProvider {

    private static final Logger log = LoggerFactory.getLogger(BunnyVideoPlaybackProvider.class);

    private final String libraryId;
    private final String tokenKey;
    private final String apiKey;
    private final int defaultTtlMinutes;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public BunnyVideoPlaybackProvider(
            @Value("${bunny.stream.library-id:}") String libraryId,
            @Value("${bunny.stream.token-key:}") String tokenKey,
            @Value("${bunny.stream.api-key:}") String apiKey,
            @Value("${app.video.embed-ttl-minutes:120}") int defaultTtlMinutes
    ) {
        this.libraryId = libraryId != null ? libraryId.trim() : "";
        this.tokenKey = tokenKey != null ? tokenKey.trim() : "";
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.defaultTtlMinutes = defaultTtlMinutes > 0 ? defaultTtlMinutes : 120;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    @Override
    public boolean isConfigured() {
        return !libraryId.isBlank() && !tokenKey.isBlank();
    }

    public int getDefaultTtlMinutes() {
        return defaultTtlMinutes;
    }

    @Override
    public SignedEmbedResult signEmbedUrl(String providerVideoId, Duration ttl) {
        if (!isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "VIDEO_PLAYBACK_NOT_CONFIGURED");
        }
        if (providerVideoId == null || providerVideoId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provider video ID is required for playback");
        }

        Duration effectiveTtl = (ttl != null && !ttl.isNegative() && !ttl.isZero())
                ? ttl
                : Duration.ofMinutes(defaultTtlMinutes);

        long expiresEpochSeconds = Instant.now().getEpochSecond() + effectiveTtl.toSeconds();
        String token = computeToken(tokenKey, providerVideoId.trim(), expiresEpochSeconds);

        String embedUrl = String.format(
                "https://iframe.mediadelivery.net/embed/%s/%s?token=%s&expires=%d",
                libraryId,
                providerVideoId.trim(),
                token,
                expiresEpochSeconds
        );

        OffsetDateTime expiresAt = Instant.ofEpochSecond(expiresEpochSeconds).atOffset(ZoneOffset.UTC);
        return new SignedEmbedResult(embedUrl, expiresAt);
    }

    /**
     * Computes the SHA256 hex token: SHA256_HEX(token_security_key + video_id + expires).
     */
    public static String computeToken(String tokenKey, String videoId, long expiresEpochSeconds) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String raw = tokenKey + videoId + expiresEpochSeconds;
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    @Override
    public Optional<VideoProviderInfo> getVideoInfo(String providerVideoId) {
        if (apiKey.isBlank() || libraryId.isBlank() || providerVideoId == null || providerVideoId.isBlank()) {
            return Optional.empty();
        }

        String url = String.format("https://video.bunnycdn.com/library/%s/videos/%s", libraryId, providerVideoId.trim());
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("AccessKey", apiKey)
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                return Optional.of(new VideoProviderInfo(false, null, false));
            }

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                int length = root.hasNonNull("length") ? root.get("length").asInt(0) : 0;
                int status = root.hasNonNull("status") ? root.get("status").asInt(-1) : -1;
                // In Bunny Stream: 4 = Finished, 8 = JitPlaylistsCreated (both signify encoding/transcoding is ready for viewing)
                boolean encodingFinished = (status == 4 || status == 8);
                return Optional.of(new VideoProviderInfo(true, length, encodingFinished));
            }

            log.warn("Video provider metadata request returned unexpected status {}: url={}", response.statusCode(), url);
            return Optional.empty();
        } catch (IOException | InterruptedException e) {
            log.warn("Video provider metadata request failed or timed out: {}", e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return Optional.empty();
        }
    }
}
