package com.allearning.backend.learning.dto;

public record AdminChapterPerformanceSummaryDto(
        Long chapterId,
        String chapterName,
        Long subjectId,
        String subjectName,
        int studentsWithData,
        int weakCount,
        int developingCount,
        int strongCount,
        Double weakPercent,
        Double averageAccuracy,
        long totalGradedAnswers
) {}

