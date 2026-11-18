package com.allearning.backend.classes.controller;

import com.allearning.backend.classes.dto.AdminRegistrationResponse;
import com.allearning.backend.classes.dto.RegistrationStatusUpdateRequest;
import com.allearning.backend.classes.service.ClassRegistrationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/class-registrations")
public class AdminClassRegistrationController {

    private final ClassRegistrationService classRegistrationService;

    public AdminClassRegistrationController(ClassRegistrationService classRegistrationService) {
        this.classRegistrationService = classRegistrationService;
    }

    /**
     * GET /api/admin/class-registrations
     * Paginated, newest first. Filters: classId, status, and q (student email or name).
     */
    @GetMapping
    public ResponseEntity<Page<AdminRegistrationResponse>> listRegistrations(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(classRegistrationService.listAdminRegistrations(classId, status, q, pageable));
    }

    /**
     * PATCH /api/admin/class-registrations/{id}/status
     * Transitions status: PENDING -> CONFIRMED, REJECTED, CANCELLED; CONFIRMED -> CANCELLED.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminRegistrationResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody RegistrationStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(classRegistrationService.updateRegistrationStatus(id, request.status()));
    }

    /**
     * GET /api/admin/class-registrations/pending-count
     * Returns count of PENDING registrations for badge display on admin navigation item.
     */
    @GetMapping("/pending-count")
    public ResponseEntity<Map<String, Long>> getPendingCount() {
        long count = classRegistrationService.getPendingRegistrationsCount();
        return ResponseEntity.ok(Map.of("count", count));
    }
}
