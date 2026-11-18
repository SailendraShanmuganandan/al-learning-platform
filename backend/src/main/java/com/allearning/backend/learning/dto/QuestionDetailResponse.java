package com.allearning.backend.learning.dto;

import com.allearning.backend.learning.entity.QuestionDifficulty;
import com.allearning.backend.learning.entity.QuestionSource;
import com.allearning.backend.learning.entity.QuestionStatus;

import java.time.OffsetDateTime;
import java.util.List;

public record QuestionDetailResponse(
        Long questionId,
        Long quizId,
        Long topicId,
        String topicName,
        String questionText,
        String explanation,
        QuestionDifficulty difficulty,
        String language,
        QuestionStatus status,
        QuestionSource source,
        Long sourceDocumentId,
        boolean sourceDocumentRemoved,
        String sourceExcerpt,
        Long createdBy,
        String createdByName,
        Long approvedBy,
        String approvedByName,
        OffsetDateTime approvedAt,
        int optionsCount,
        long answerCount,
        Double correctRate,
        long openReportCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<QuestionOptionResponse> options
) {}

