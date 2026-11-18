package com.allearning.backend.learning.dto;

import com.allearning.backend.learning.entity.MistakeStatus;

import java.time.OffsetDateTime;

public record MistakeItemResponse(
        Long questionId,
        String questionText,
        Long chapterId,
        String chapterName,
        Long subjectId,
        String subjectName,
        String topicName,
        MistakeStatus status,
        int correctStreak,
        OffsetDateTime lastAnsweredAt
) {}

