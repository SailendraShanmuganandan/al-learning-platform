package com.allearning.backend.learning.controller;

import com.allearning.backend.learning.dto.SyllabusProgressResponse;
import com.allearning.backend.learning.service.AnalyticsService;
import com.allearning.backend.learning.service.LearningUserLookupService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/me/progress")
public class ProgressController {

    private final AnalyticsService analyticsService;
    private final LearningUserLookupService userLookupService;

    public ProgressController(
            AnalyticsService analyticsService,
            LearningUserLookupService userLookupService
    ) {
        this.analyticsService = analyticsService;
        this.userLookupService = userLookupService;
    }

    @GetMapping
    public ResponseEntity<SyllabusProgressResponse> getProgress(
            @RequestParam(required = false) Long subjectId,
            Authentication authentication
    ) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(analyticsService.getProgress(userId, subjectId));
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

