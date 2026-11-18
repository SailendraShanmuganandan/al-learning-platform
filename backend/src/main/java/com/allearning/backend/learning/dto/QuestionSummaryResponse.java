package com.allearning.backend.learning.dto;

import com.allearning.backend.learning.entity.QuestionDifficulty;
import com.allearning.backend.learning.entity.QuestionSource;
import com.allearning.backend.learning.entity.QuestionStatus;

import java.time.OffsetDateTime;

public record QuestionSummaryResponse(
        Long questionId,
        Long quizId,
        Long chapterId,
        String chapterName,
        String subjectName,
        Long topicId,
        String topicName,
        String questionText,
        QuestionDifficulty difficulty,
        String language,
        QuestionStatus status,
        QuestionSource source,
        Long sourceDocumentId,
        boolean sourceDocumentRemoved,
        int optionsCount,
        long answerCount,
        Double correctRate,
        long openReportCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public QuestionSummaryResponse(
            Long questionId,
            Long quizId,
            Long topicId,
            String topicName,
            String questionText,
            QuestionDifficulty difficulty,
            String language,
            QuestionStatus status,
            QuestionSource source,
            Long sourceDocumentId,
            boolean sourceDocumentRemoved,
            int optionsCount,
            long answerCount,
            Double correctRate,
            long openReportCount,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this(questionId, quizId, null, null, null, topicId, topicName, questionText, difficulty, language,
                status, source, sourceDocumentId, sourceDocumentRemoved, optionsCount, answerCount,
                correctRate, openReportCount, createdAt, updatedAt);
    }
}
