package com.allearning.backend.catalog.dto;

import com.allearning.backend.catalog.entity.ProductStatus;

/**
 * Internal projection containing video product facts for commerce/ownership modules.
 * Satisfies modular-monolith boundary requirements: never exposed in public DTOs.
 */
public record VideoProductFacts(
        Long productId,
        String name,
        String slug,
        String thumbnailUrl,
        Integer durationSeconds,
        String description,
        ProductStatus status,
        String providerVideoId,
        boolean hasAttachmentPdf,
        String attachmentPdfName,
        Long attachmentPdfSize
) {
}
