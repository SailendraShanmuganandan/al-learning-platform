package com.allearning.backend.learning.dto;

import com.allearning.backend.learning.entity.QuizAttemptType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AttemptDetailResponse(
        Long attemptId,
        QuizAttemptType attemptType,
        Long quizId,
        Long chapterId,
        String status,
        String language,
        Integer score,
        int totalMarks,
        Double percentage,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        Boolean resumed,
        List<ChapterBreakdown> chapterBreakdown,
        List<?> questions
) {}

