package com.allearning.backend.catalog.dto;

import com.allearning.backend.catalog.entity.Product;
import com.allearning.backend.catalog.entity.ProductStatus;
import com.allearning.backend.catalog.entity.ProductType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ProductSummaryResponse(
        Long productId,
        String name,
        String slug,
        ProductType productType,
        BigDecimal price,
        ProductStatus status,
        String thumbnailUrl,
        Integer stockQuantity,
        Integer pageCount,
        Integer durationSeconds,
        Boolean hasAttachmentPdf,
        int chapterCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ProductSummaryResponse from(Product product) {
        Integer stock = product.getNote() != null ? product.getNote().getStockQuantity() : null;
        Integer pages = product.getNote() != null ? product.getNote().getPageCount() : null;
        Integer duration = product.getVideo() != null ? product.getVideo().getDurationSeconds() : null;
        Boolean hasPdf = product.getVideo() != null && product.getVideo().getPdfFilePath() != null && !product.getVideo().getPdfFilePath().isBlank();
        int chapters = product.getChapterIds() != null ? product.getChapterIds().size() : 0;

        return new ProductSummaryResponse(
                product.getProductId(),
                product.getName(),
                product.getSlug(),
                product.getProductType(),
                product.getPrice(),
                product.getStatus(),
                product.getThumbnailUrl(),
                stock,
                pages,
                duration,
                hasPdf,
                chapters,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
