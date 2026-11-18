package com.allearning.backend.learning.dto;

import java.util.List;

public record SyllabusProgressResponse(
        ProgressSummary overall,
        List<SubjectProgressDto> subjects
) {}

