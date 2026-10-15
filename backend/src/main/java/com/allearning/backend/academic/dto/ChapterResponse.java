package com.allearning.backend.academic.dto;

import com.allearning.backend.academic.entity.Chapter;
import java.time.OffsetDateTime;

/**
 * Outbound DTO for chapter responses.
 * Includes the owning subject's ID so callers can navigate back if needed.
 */
public record ChapterResponse(
        Long chapterId,
        Long subjectId,
        String name,
        Integer chapterOrder,
        String description,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ChapterResponse from(Chapter chapter) {
        return new ChapterResponse(
                chapter.getChapterId(),
                chapter.getSubject().getSubjectId(),
                chapter.getName(),
                chapter.getChapterOrder(),
                chapter.getDescription(),
                chapter.getStatus(),
                chapter.getCreatedAt(),
                chapter.getUpdatedAt()
        );
    }
}
