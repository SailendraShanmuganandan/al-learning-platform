package com.allearning.backend.classes.controller;

import com.allearning.backend.classes.dto.AdminClassResponse;
import com.allearning.backend.classes.dto.ClassStatusUpdateRequest;
import com.allearning.backend.classes.dto.CreateClassRequest;
import com.allearning.backend.classes.dto.UpdateClassRequest;
import com.allearning.backend.classes.service.ClassService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/classes")
public class AdminClassController {

    private final ClassService classService;

    public AdminClassController(ClassService classService) {
        this.classService = classService;
    }

    /**
     * GET /api/admin/classes
     * Returns all classes including INACTIVE, with counts of PENDING and CONFIRMED registrations.
     */
    @GetMapping
    public ResponseEntity<List<AdminClassResponse>> listClasses() {
        return ResponseEntity.ok(classService.listAdminClasses());
    }

    /**
     * GET /api/admin/classes/{id}
     * Full details for a single class.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AdminClassResponse> getClassById(@PathVariable Long id) {
        return ResponseEntity.ok(classService.getAdminClass(id));
    }

    /**
     * POST /api/admin/classes
     * Creates a new class (subject must exist and be ACTIVE).
     */
    @PostMapping
    public ResponseEntity<AdminClassResponse> createClass(@Valid @RequestBody CreateClassRequest request) {
        AdminClassResponse created = classService.createClass(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PATCH /api/admin/classes/{id}
     * Edits mutable fields. subject_id cannot be changed.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<AdminClassResponse> updateClass(
            @PathVariable Long id,
            @Valid @RequestBody UpdateClassRequest request
    ) {
        return ResponseEntity.ok(classService.updateClass(id, request));
    }

    /**
     * PATCH /api/admin/classes/{id}/status
     * Sets class status to ACTIVE or INACTIVE.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminClassResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ClassStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(classService.setClassStatus(id, request.status()));
    }
}
