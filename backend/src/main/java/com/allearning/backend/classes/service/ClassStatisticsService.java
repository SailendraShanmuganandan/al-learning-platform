package com.allearning.backend.classes.service;

import com.allearning.backend.classes.dto.ClassStatisticsDto;

/**
 * Service interface for aggregate classes domain statistics.
 */
public interface ClassStatisticsService {
    ClassStatisticsDto getClassStatistics();
}
