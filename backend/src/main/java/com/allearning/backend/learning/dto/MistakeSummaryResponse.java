package com.allearning.backend.learning.dto;

import java.util.List;

public record MistakeSummaryResponse(
        long totalActive,
        long totalPracticeable,
        long totalMastered,
        List<ChapterMistakeSummary> byChapter
) {
    public List<ChapterMistakeSummary> chapterBreakdown() { return byChapter; }
}
