package com.allearning.backend.catalog.dto;

import com.allearning.backend.catalog.entity.ProductStatus;
import jakarta.validation.constraints.NotNull;

public record ProductStatusRequest(
        @NotNull(message = "Status is required")
        ProductStatus status
) {}
