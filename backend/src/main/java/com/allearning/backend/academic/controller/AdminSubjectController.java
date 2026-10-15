package com.allearning.backend.academic.controller;

import com.allearning.backend.academic.dto.SubjectRequest;
import com.allearning.backend.academic.dto.SubjectResponse;
import com.allearning.backend.academic.dto.SubjectStatusRequest;
import com.allearning.backend.academic.service.SubjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin REST controller for managing subjects.
 * Access is restricted to ADMIN role via {@link com.allearning.backend.security.SecurityConfig}.
 */
@RestController
@RequestMapping("/api/admin/subjects")
public class AdminSubjectController {

    private final SubjectService subjectService;

    public AdminSubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    /**
     * GET /api/admin/subjects
     * Returns all subjects, including INACTIVE, ordered by name.
     */
    @GetMapping
    public ResponseEntity<List<SubjectResponse>> listAll() {
        return ResponseEntity.ok(subjectService.listAll());
    }

    /**
     * POST /api/admin/subjects
     * Creates a new ACTIVE subject.
     * Returns 409 if name already exists (case-insensitive).
     */
    @PostMapping
    public ResponseEntity<SubjectResponse> create(@Valid @RequestBody SubjectRequest request) {
        SubjectResponse response = subjectService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PATCH /api/admin/subjects/{id}
     * Updates name and/or description of a subject.
     * Returns 409 if new name conflicts with an existing subject.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<SubjectResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SubjectRequest request
    ) {
        return ResponseEntity.ok(subjectService.update(id, request));
    }

    /**
     * PATCH /api/admin/subjects/{id}/status
     * Sets the status of a subject to ACTIVE or INACTIVE.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<SubjectResponse> setStatus(
            @PathVariable Long id,
            @Valid @RequestBody SubjectStatusRequest request
    ) {
        return ResponseEntity.ok(subjectService.setStatus(id, request.status()));
    }
}
