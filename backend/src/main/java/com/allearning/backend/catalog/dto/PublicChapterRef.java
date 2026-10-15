package com.allearning.backend.catalog.dto;

/**
 * Lightweight chapter reference used in public product responses.
 * Only publicly visible chapters (ACTIVE chapter + ACTIVE subject) are included.
 * Never exposes academic internals.
 */
public record PublicChapterRef(
        Long chapterId,
        String name,
        String subjectName
) {}
