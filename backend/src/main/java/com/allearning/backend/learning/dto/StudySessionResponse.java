package com.allearning.backend.learning.dto;

import com.allearning.backend.learning.entity.StudySessionStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record StudySessionResponse(
        Long sessionId,
        int durationMinutes,
        StudySessionStatus status,
        Boolean resumed,
        OffsetDateTime startedAt,
        OffsetDateTime endsAt,
        long remainingSeconds,
        Long attemptId,
        int questionCount,
        List<StudySessionItemResponse> items,
        StudySessionResultDto result
) {
    public record StudySessionResultDto(
            int score,
            int totalMarks,
            int percentage
    ) {}
}

