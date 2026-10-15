package com.allearning.backend.catalog.dto;

import com.allearning.backend.catalog.entity.Product;
import com.allearning.backend.catalog.entity.ProductStatus;
import com.allearning.backend.catalog.entity.ProductType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public record ProductDetailResponse(
        Long productId,
        String name,
        String slug,
        String description,
        ProductType productType,
        BigDecimal price,
        ProductStatus status,
        String thumbnailUrl,
        List<Long> chapterIds,
        // Note subtype fields
        Integer pageCount,
        Integer stockQuantity,
        // Video subtype fields
        String videoUrl,
        Integer durationSeconds,
        String videoDescription,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ProductDetailResponse from(Product product) {
        Integer pageCount = null;
        Integer stockQuantity = null;
        if (product.getNote() != null) {
            pageCount = product.getNote().getPageCount();
            stockQuantity = product.getNote().getStockQuantity();
        }

        String videoUrl = null;
        Integer durationSeconds = null;
        String videoDescription = null;
        if (product.getVideo() != null) {
            videoUrl = product.getVideo().getVideoUrl();
            durationSeconds = product.getVideo().getDurationSeconds();
            videoDescription = product.getVideo().getDescription();
        }

        List<Long> chapters = product.getChapterIds() != null
                ? new ArrayList<>(product.getChapterIds())
                : List.of();

        return new ProductDetailResponse(
                product.getProductId(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getProductType(),
                product.getPrice(),
                product.getStatus(),
                product.getThumbnailUrl(),
                chapters,
                pageCount,
                stockQuantity,
                videoUrl,
                durationSeconds,
                videoDescription,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
