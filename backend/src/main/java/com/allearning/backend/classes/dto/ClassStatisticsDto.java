package com.allearning.backend.classes.dto;

import java.util.List;

public record ClassStatisticsDto(
        long activeClasses,
        long pendingRegistrations,
        long confirmedRegistrations,
        List<RecentRegistrationDto> recentRegistrations
) {
}
