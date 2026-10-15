package com.allearning.backend.academic.controller;

import com.allearning.backend.academic.dto.ChapterResponse;
import com.allearning.backend.academic.service.ChapterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public REST controller for chapter endpoints.
 * No authentication required — security is enforced in
 * {@link com.allearning.backend.security.SecurityConfig}.
 */
@RestController
public class ChapterController {

    private final ChapterService chapterService;

    public ChapterController(ChapterService chapterService) {
        this.chapterService = chapterService;
    }

    /**
     * GET /api/subjects/{subjectId}/chapters
     * Returns only ACTIVE chapters of an ACTIVE subject, sorted by chapter_order.
     * Returns 404 if the subject is missing or INACTIVE.
     */
    @GetMapping("/api/subjects/{subjectId}/chapters")
    public ResponseEntity<List<ChapterResponse>> listActiveChapters(
            @PathVariable Long subjectId) {
        return ResponseEntity.ok(chapterService.listActiveForSubject(subjectId));
    }

    /**
     * GET /api/chapters/{id}
     * Returns a single ACTIVE chapter.
     * Returns 404 if the chapter is INACTIVE, missing, or belongs to an INACTIVE subject.
     */
    @GetMapping("/api/chapters/{id}")
    public ResponseEntity<ChapterResponse> getActiveChapter(@PathVariable Long id) {
        return ResponseEntity.ok(chapterService.getActiveById(id));
    }
}
