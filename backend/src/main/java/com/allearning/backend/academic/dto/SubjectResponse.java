package com.allearning.backend.academic.dto;

import com.allearning.backend.academic.entity.Subject;
import java.time.OffsetDateTime;

/**
 * Outbound DTO for subject responses. Never exposes internal implementation details.
 */
public record SubjectResponse(
        Long subjectId,
        String name,
        String description,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static SubjectResponse from(Subject subject) {
        return new SubjectResponse(
                subject.getSubjectId(),
                subject.getName(),
                subject.getDescription(),
                subject.getStatus(),
                subject.getCreatedAt(),
                subject.getUpdatedAt()
        );
    }
}
