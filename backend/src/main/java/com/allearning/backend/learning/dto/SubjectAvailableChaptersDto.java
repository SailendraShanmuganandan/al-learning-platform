package com.allearning.backend.learning.dto;

import java.util.List;

public record SubjectAvailableChaptersDto(
        Long subjectId,
        String subjectName,
        List<AvailableChapterDto> chapters
) {}

