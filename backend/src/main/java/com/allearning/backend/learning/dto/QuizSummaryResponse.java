package com.allearning.backend.learning.dto;

import com.allearning.backend.learning.entity.QuizStatus;

import java.time.OffsetDateTime;
import java.util.Map;

public record QuizSummaryResponse(
        Long quizId,
        Long chapterId,
        String chapterName,
        Long subjectId,
        String subjectName,
        String title,
        String description,
        QuizStatus status,
        long questionCount,
        long approvedCount,
        long pendingCount,
        long eligibleCount,
        Map<String, Long> eligibleByLanguage,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public QuizSummaryResponse(
            Long quizId,
            Long chapterId,
            String chapterName,
            Long subjectId,
            String subjectName,
            String title,
            String description,
            QuizStatus status,
            long questionCount,
            long approvedCount,
            long eligibleCount,
            Map<String, Long> eligibleByLanguage,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this(quizId, chapterId, chapterName, subjectId, subjectName, title, description, status,
                questionCount, approvedCount, 0L, eligibleCount, eligibleByLanguage, createdAt, updatedAt);
    }
}
