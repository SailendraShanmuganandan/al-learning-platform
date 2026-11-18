package com.allearning.backend.catalog.dto;

import com.allearning.backend.academic.service.ChapterValidationService.PublicChapterSummary;
import com.allearning.backend.catalog.entity.Note;
import com.allearning.backend.catalog.entity.Product;
import com.allearning.backend.catalog.entity.ProductType;
import com.allearning.backend.catalog.entity.Video;

import java.math.BigDecimal;
import java.util.List;

/**
 * Public product detail response — used by unauthenticated/authenticated public endpoints.
 *
 * Security rules enforced here:
 * - {@code video_url} is NEVER included (see FR-VID-06).
 * - {@code stock_quantity} is NEVER included; in_stock boolean is used instead.
 * - Only publicly visible chapters (ACTIVE chapter + ACTIVE subject) are linked.
 */
public record PublicProductDetailResponse(
        Long id,
        String name,
        String slug,
        String description,
        ProductType type,
        BigDecimal price,
        String thumbnailUrl,
        List<PublicChapterRef> chapters,

        // Note-specific (null for VIDEO)
        Integer pageCount,
        Boolean inStock,

        // Video-specific (null for PHYSICAL_NOTE). video_url intentionally omitted.
        Integer durationSeconds,
        String videoDescription,
        Boolean hasAttachmentPdf,
        String attachmentPdfName,
        Long attachmentPdfSize
) {
    /**
     * Builds a public response from the product and a pre-resolved list of public chapter summaries.
     * The caller is responsible for filtering {@code publicChapters} to only publicly visible ones.
     */
    public static PublicProductDetailResponse from(
            Product product,
            List<PublicChapterSummary> publicChapters) {

        List<PublicChapterRef> chapterRefs = publicChapters.stream()
                .map(c -> new PublicChapterRef(c.chapterId(), c.name(), c.subjectName()))
                .toList();

        // Note fields
        Integer pageCount = null;
        Boolean inStock = null;
        if (product.getProductType() == ProductType.PHYSICAL_NOTE) {
            Note note = product.getNote();
            pageCount = (note != null) ? note.getPageCount() : null;
            inStock = (note != null) && (note.getStockQuantity() != null && note.getStockQuantity() > 0);
        }

        // Video fields — video_url intentionally excluded
        Integer durationSeconds = null;
        String videoDescription = null;
        Boolean hasAttachmentPdf = false;
        String attachmentPdfName = null;
        Long attachmentPdfSize = null;
        if (product.getProductType() == ProductType.VIDEO) {
            Video video = product.getVideo();
            durationSeconds = (video != null) ? video.getDurationSeconds() : null;
            videoDescription = (video != null) ? video.getDescription() : null;
            hasAttachmentPdf = (video != null && video.getPdfFilePath() != null && !video.getPdfFilePath().isBlank());
            attachmentPdfName = (video != null) ? video.getPdfOriginalFilename() : null;
            attachmentPdfSize = (video != null) ? video.getPdfFileSize() : null;
        }

        return new PublicProductDetailResponse(
                product.getProductId(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getProductType(),
                product.getPrice(),
                product.getThumbnailUrl(),
                chapterRefs,
                pageCount,
                inStock,
                durationSeconds,
                videoDescription,
                hasAttachmentPdf,
                attachmentPdfName,
                attachmentPdfSize
        );
    }
}
