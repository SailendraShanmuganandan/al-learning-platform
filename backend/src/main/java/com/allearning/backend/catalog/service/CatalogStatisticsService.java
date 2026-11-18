package com.allearning.backend.catalog.service;

import com.allearning.backend.catalog.dto.CatalogStatisticsDto;

/**
 * Service interface for aggregate catalog domain statistics.
 */
public interface CatalogStatisticsService {
    CatalogStatisticsDto getCatalogStatistics(int lowStockThreshold);
}
