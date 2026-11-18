package com.allearning.backend.commerce.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * Summary DTO for owned videos in the student's "My Videos" library.
 * Security: NEVER includes provider video IDs, tokens, or embed URLs.
 */
public record MyVideoSummaryResponse(
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

        @JsonProperty("hasAttachmentPdf")
        Boolean hasAttachmentPdf
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
