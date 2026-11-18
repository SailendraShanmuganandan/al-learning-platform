package com.allearning.backend.learning.dto;

import com.allearning.backend.learning.entity.AssessmentScopeType;
import com.allearning.backend.learning.entity.AssessmentType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AssessmentDetailResponse(
        Long assessmentId,
        AssessmentType assessmentType,
        AssessmentScopeType scopeType,
        String status,
        Integer score,
        int totalMarks,
        Double percentage,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        List<ChapterBreakdown> chapterBreakdown,
        List<?> questions
) {}

