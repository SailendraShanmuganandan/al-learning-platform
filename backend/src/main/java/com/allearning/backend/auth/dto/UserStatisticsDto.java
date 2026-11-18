package com.allearning.backend.auth.dto;

public record UserStatisticsDto(
        long totalStudents,
        long newStudentsLast30Days,
        long unverifiedStudents
) {
}
