package com.allearning.backend.learning.dto;

public record AvailableChapterDto(
        Long chapterId,
        String name,
        Integer chapterOrder,
        long eligibleCount
) {}

