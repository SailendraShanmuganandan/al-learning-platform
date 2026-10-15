package com.allearning.backend.academic.controller;

import com.allearning.backend.academic.dto.*;
import com.allearning.backend.academic.service.ChapterService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin REST controller for managing chapters.
 * Access is restricted to ADMIN role via {@link com.allearning.backend.security.SecurityConfig}.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminChapterController {

    private final ChapterService chapterService;

    public AdminChapterController(ChapterService chapterService) {
        this.chapterService = chapterService;
    }

    /**
     * GET /api/admin/chapters
     * Returns all chapters across all subjects for admin view (e.g. for multi-selects).
     */
    @GetMapping("/chapters")
    public ResponseEntity<List<ChapterResponse>> listAllChapters() {
        return ResponseEntity.ok(chapterService.listAll());
    }

    /**
     * GET /api/admin/subjects/{subjectId}/chapters
     * Returns all chapters (including INACTIVE) for a subject, sorted by chapter_order.
     */
    @GetMapping("/subjects/{subjectId}/chapters")
    public ResponseEntity<List<ChapterResponse>> listAllChaptersForSubject(
            @PathVariable Long subjectId) {
        return ResponseEntity.ok(chapterService.listAllForSubject(subjectId));
    }

    /**
     * POST /api/admin/chapters
     * Creates a new chapter. If chapter_order is omitted, auto-appends.
     * Returns 409 if the given order is already taken in the same subject.
     */
    @PostMapping("/chapters")
    public ResponseEntity<ChapterResponse> create(
            @Valid @RequestBody ChapterCreateRequest request) {
        ChapterResponse response = chapterService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PATCH /api/admin/chapters/{id}
     * Updates name, description, and/or chapter_order.
     * Returns 409 if the new order conflicts with an existing chapter in the same subject.
     * subject_id cannot be changed.
     */
    @PatchMapping("/chapters/{id}")
    public ResponseEntity<ChapterResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ChapterUpdateRequest request) {
        return ResponseEntity.ok(chapterService.update(id, request));
    }

    /**
     * PATCH /api/admin/chapters/{id}/status
     * Sets a chapter's status to ACTIVE or INACTIVE.
     */
    @PatchMapping("/chapters/{id}/status")
    public ResponseEntity<ChapterResponse> setStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChapterStatusRequest request) {
        return ResponseEntity.ok(chapterService.setStatus(id, request.status()));
    }
}
