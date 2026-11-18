package com.allearning.backend.commerce.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * Playback response for an owned video.
 * Returned only to the student who owns the product.
 * Security: Contains temporary signed embed URL and expiration; NEVER provider credentials or keys.
 */
public record MyVideoDetailResponse(
        @JsonProperty("productId")
        Long productId,

        @JsonProperty("slug")
        String slug,

        @JsonProperty("name")
        String name,

        @JsonProperty("thumbnail_url")
        String thumbnailUrl,

        @JsonProperty("duration_seconds")
        Integer durationSeconds,

        @JsonProperty("description")
        String description,

        @JsonProperty("purchased_at")
        OffsetDateTime purchasedAt,

        @JsonProperty("embedUrl")
        String embedUrl,

        @JsonProperty("expiresAt")
        OffsetDateTime expiresAt,

        @JsonProperty("hasAttachmentPdf")
        Boolean hasAttachmentPdf,

        @JsonProperty("attachmentPdfName")
        String attachmentPdfName,

        @JsonProperty("attachmentPdfSize")
        Long attachmentPdfSize
) {
    @JsonProperty("thumbnailUrl")
    public String getThumbnailUrlCamel() {
        return thumbnailUrl;
    }

    @JsonProperty("durationSeconds")
    public Integer getDurationSecondsCamel() {
        return durationSeconds;
    }

    @JsonProperty("purchasedAt")
    public OffsetDateTime getPurchasedAtCamel() {
        return purchasedAt;
    }
}
