package com.allearning.backend.catalog.dto;

import com.allearning.backend.catalog.entity.Note;
import com.allearning.backend.catalog.entity.Product;
import com.allearning.backend.catalog.entity.ProductType;
import com.allearning.backend.catalog.entity.Video;

import java.math.BigDecimal;

/**
 * Compact public product summary for list endpoints.
 *
 * Security rules enforced here:
 * - {@code video_url} is NEVER included.
 * - {@code stock_quantity} is NEVER included; in_stock boolean used instead for notes.
 */
public record PublicProductSummaryResponse(
        Long id,
        String name,
        String slug,
        ProductType type,
        BigDecimal price,
        String thumbnailUrl,

        // Note-specific (null for VIDEO)
        Integer pageCount,
        Boolean inStock,

        // Video-specific (null for PHYSICAL_NOTE)
        Integer durationSeconds,

        int chapterCount
) {
    public static PublicProductSummaryResponse from(Product product) {
        Integer pageCount = null;
        Boolean inStock = null;
        if (product.getProductType() == ProductType.PHYSICAL_NOTE) {
            Note note = product.getNote();
            pageCount = (note != null) ? note.getPageCount() : null;
            inStock = (note != null) && (note.getStockQuantity() != null && note.getStockQuantity() > 0);
        }

        Integer durationSeconds = null;
        if (product.getProductType() == ProductType.VIDEO) {
            Video video = product.getVideo();
            durationSeconds = (video != null) ? video.getDurationSeconds() : null;
        }

        int chapterCount = product.getChapterIds() != null ? product.getChapterIds().size() : 0;

        return new PublicProductSummaryResponse(
                product.getProductId(),
                product.getName(),
                product.getSlug(),
                product.getProductType(),
                product.getPrice(),
                product.getThumbnailUrl(),
                pageCount,
                inStock,
                durationSeconds,
                chapterCount
        );
    }
}
