package com.allearning.backend.academic.controller;

import com.allearning.backend.academic.dto.SubjectResponse;
import com.allearning.backend.academic.service.SubjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public REST controller for subject endpoints.
 * No authentication required — security is enforced in {@link com.allearning.backend.security.SecurityConfig}.
 */
@RestController
@RequestMapping("/api/subjects")
public class SubjectController {

    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    /**
     * GET /api/subjects
     * Returns all ACTIVE subjects ordered by name.
     */
    @GetMapping
    public ResponseEntity<List<SubjectResponse>> listActive() {
        return ResponseEntity.ok(subjectService.listActive());
    }

    /**
     * GET /api/subjects/{id}
     * Returns a single ACTIVE subject. Returns 404 if INACTIVE or missing.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SubjectResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(subjectService.getActiveById(id));
    }
}
