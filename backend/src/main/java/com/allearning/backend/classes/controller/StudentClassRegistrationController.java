package com.allearning.backend.classes.controller;

import com.allearning.backend.classes.dto.StudentRegistrationResponse;
import com.allearning.backend.classes.service.ClassRegistrationService;
import com.allearning.backend.classes.service.StudentLookupService;
import com.allearning.backend.security.RequiresEmailVerified;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class StudentClassRegistrationController {

    private final ClassRegistrationService classRegistrationService;
    private final StudentLookupService studentLookupService;

    public StudentClassRegistrationController(
            ClassRegistrationService classRegistrationService,
            StudentLookupService studentLookupService
    ) {
        this.classRegistrationService = classRegistrationService;
        this.studentLookupService = studentLookupService;
    }

    /**
     * POST /api/classes/{classId}/register
     * Registers the authenticated student for an ACTIVE class.
     * Enforces email verification via @RequiresEmailVerified (FR-AUTH-14).
     */
    @PostMapping("/api/classes/{classId}/register")
    @RequiresEmailVerified
    public ResponseEntity<StudentRegistrationResponse> register(
            Authentication authentication,
            @PathVariable Long classId
    ) {
        Long userId = extractUserId(authentication);
        StudentRegistrationResponse response = classRegistrationService.register(userId, classId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/my/class-registrations
     * Returns the authenticated student's registrations, newest first.
     */
    @GetMapping("/api/my/class-registrations")
    public ResponseEntity<List<StudentRegistrationResponse>> getMyRegistrations(Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(classRegistrationService.getStudentRegistrations(userId));
    }

    /**
     * GET /api/my/class-registrations/{id}
     * Returns details of a specific registration owned by the authenticated student.
     */
    @GetMapping("/api/my/class-registrations/{id}")
    public ResponseEntity<StudentRegistrationResponse> getMyRegistration(
            Authentication authentication,
            @PathVariable Long id
    ) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(classRegistrationService.getStudentRegistration(userId, id));
    }

    /**
     * PATCH /api/my/class-registrations/{id}/cancel
     * Allows student to cancel a PENDING registration (FR-CLASS-08).
     */
    @PatchMapping("/api/my/class-registrations/{id}/cancel")
    public ResponseEntity<StudentRegistrationResponse> cancelRegistration(
            Authentication authentication,
            @PathVariable Long id
    ) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(classRegistrationService.cancelStudentRegistration(userId, id));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        String email = authentication.getName();
        Long userId = studentLookupService.findUserIdByEmail(email);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
        }
        return userId;
    }
}
