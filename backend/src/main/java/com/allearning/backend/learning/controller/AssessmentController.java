package com.allearning.backend.learning.controller;

import com.allearning.backend.learning.dto.*;
import com.allearning.backend.learning.entity.AssessmentStatus;
import com.allearning.backend.learning.entity.AssessmentType;
import com.allearning.backend.learning.service.AssessmentService;
import com.allearning.backend.learning.service.LearningUserLookupService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AssessmentController {

    private final AssessmentService assessmentService;
    private final LearningUserLookupService userLookupService;

    public AssessmentController(
            AssessmentService assessmentService,
            LearningUserLookupService userLookupService
    ) {
        this.assessmentService = assessmentService;
        this.userLookupService = userLookupService;
    }

    @PostMapping("/diagnostic-assessments")
    public ResponseEntity<AssessmentDetailResponse> createAssessment(
            @Valid @RequestBody AssessmentCreateRequest request,
            Authentication authentication
    ) {
        Long userId = extractUserId(authentication);
        AssessmentDetailResponse response = assessmentService.createAssessment(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/diagnostic-assessments/{id}")
    public ResponseEntity<AssessmentDetailResponse> getAssessment(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(assessmentService.getAssessmentDetail(userId, id));
    }

    @PostMapping("/diagnostic-assessments/{id}/submit")
    public ResponseEntity<AssessmentDetailResponse> submitAssessment(
            @PathVariable Long id,
            @Valid @RequestBody SubmitAnswersRequest request,
            Authentication authentication
    ) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(assessmentService.submitAssessment(userId, id, request));
    }

    @GetMapping("/me/diagnostic-assessments")
    public ResponseEntity<Page<AssessmentDetailResponse>> getMyAssessments(
            @RequestParam(required = false) AssessmentType type,
            @RequestParam(required = false) AssessmentStatus status,
            @PageableDefault(size = 10) @SortDefault(sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication
    ) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(assessmentService.getMyAssessments(userId, type, status, pageable));
    }

    @GetMapping("/me/diagnostic-assessments/onboarding-status")
    public ResponseEntity<OnboardingStatusResponse> getOnboardingStatus(Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(assessmentService.getOnboardingStatus(userId));
    }

    @GetMapping("/assessments/available-chapters")
    public ResponseEntity<List<SubjectAvailableChaptersDto>> getAvailableChapters() {
        return ResponseEntity.ok(assessmentService.getAvailableChapters());
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        String email = authentication.getName();
        Long userId = userLookupService.findUserIdByEmail(email);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
        }
        return userId;
    }
}

