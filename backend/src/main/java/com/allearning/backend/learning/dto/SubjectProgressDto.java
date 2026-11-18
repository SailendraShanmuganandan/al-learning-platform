package com.allearning.backend.learning.dto;

import java.util.List;

public record SubjectProgressDto(
        Long subjectId,
        String subjectName,
        ProgressSummary summary,
        List<ChapterProgressDto> chapters
) {}

