package com.allearning.backend.auth.service;

import com.allearning.backend.auth.dto.UserStatisticsDto;

import java.time.ZoneId;

/**
 * Service interface for aggregate user domain statistics.
 * Adheres to modular monolith boundaries: does not expose internal entities.
 */
public interface UserStatisticsService {
    UserStatisticsDto getUserStatistics(ZoneId zone);
}
