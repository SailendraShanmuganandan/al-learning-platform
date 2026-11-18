package com.allearning.backend.learning.dto;

import com.allearning.backend.learning.entity.AssessmentScopeType;
import com.allearning.backend.learning.entity.AssessmentType;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AssessmentCreateRequest(
        @NotNull(message = "Assessment type is required")
        AssessmentType assessmentType,
        @NotNull(message = "Scope type is required")
        AssessmentScopeType scopeType,
        List<Long> chapterIds,
        Long subjectId,
        String language
) {}

