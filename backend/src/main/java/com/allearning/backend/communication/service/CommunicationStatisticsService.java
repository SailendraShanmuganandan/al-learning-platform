package com.allearning.backend.communication.service;

import com.allearning.backend.communication.dto.CommunicationStatisticsDto;

/**
 * Service interface for aggregate communication domain statistics.
 */
public interface CommunicationStatisticsService {
    CommunicationStatisticsDto getCommunicationStatistics();
}
