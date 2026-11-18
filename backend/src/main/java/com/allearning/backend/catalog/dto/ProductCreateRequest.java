package com.allearning.backend.catalog.dto;

import com.allearning.backend.catalog.entity.ProductType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record ProductCreateRequest(
        @NotBlank(message = "Product name is required")
        @Size(max = 300, message = "Product name must be at most 300 characters")
        String name,

        String description,

        @NotNull(message = "Product type is required")
        ProductType productType,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Price must be non-negative")
        BigDecimal price,

        String thumbnailUrl,

        List<Long> chapterIds,

        // Note-specific fields
        Integer pageCount,
        Integer stockQuantity,

        // Video-specific fields
        String providerVideoId,
        Integer durationSeconds,
        String videoDescription
) {}
