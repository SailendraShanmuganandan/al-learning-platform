package com.allearning.backend.catalog.dto;

import java.util.List;

public record CatalogStatisticsDto(
        long publishedNotes,
        long publishedVideos,
        long draftProducts,
        long lowStockCount,
        List<LowStockItemDto> lowStockItems
) {
}
