package com.allearning.backend.catalog.dto;

import com.allearning.backend.catalog.entity.ProductType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record ProductUpdateRequest(
        @Size(max = 300, message = "Product name must be at most 300 characters")
        String name,

        String description,

        @DecimalMin(value = "0.0", inclusive = true, message = "Price must be non-negative")
        BigDecimal price,

        String thumbnailUrl,

        List<Long> chapterIds,

        // Note-specific fields
        Integer pageCount,
        Integer stockQuantity,

        // Video-specific fields
        String videoUrl,
        Integer durationSeconds,
        String videoDescription,

        // If provided, must match existing type
        ProductType productType
) {}
