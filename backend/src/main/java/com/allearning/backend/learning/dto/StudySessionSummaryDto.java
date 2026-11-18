package com.allearning.backend.learning.dto;

import com.allearning.backend.learning.entity.StudySessionStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record StudySessionSummaryDto(
        Long sessionId,
        int durationMinutes,
        StudySessionStatus status,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        Integer score,
        Integer totalMarks
) {}

