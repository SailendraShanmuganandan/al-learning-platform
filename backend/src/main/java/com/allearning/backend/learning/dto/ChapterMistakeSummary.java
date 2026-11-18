package com.allearning.backend.learning.dto;

public record ChapterMistakeSummary(
        Long chapterId,
        String chapterName,
        Long subjectId,
        String subjectName,
        long activeCount,
        long practiceableCount
) {}

