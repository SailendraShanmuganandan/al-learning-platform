package com.allearning.backend.learning.controller;

import com.allearning.backend.learning.dto.ChapterAnalyticsDetailResponse;
import com.allearning.backend.learning.dto.ChapterPerformanceDto;
import com.allearning.backend.learning.dto.WeakAreasResponse;
import com.allearning.backend.learning.service.AnalyticsService;
import com.allearning.backend.learning.service.LearningUserLookupService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/me/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final LearningUserLookupService userLookupService;

    public AnalyticsController(
            AnalyticsService analyticsService,
            LearningUserLookupService userLookupService
    ) {
        this.analyticsService = analyticsService;
        this.userLookupService = userLookupService;
    }

    @GetMapping("/chapters")
    public ResponseEntity<List<ChapterPerformanceDto>> getChapterPerformances(
            @RequestParam(required = false) Long subjectId,
            Authentication authentication
    ) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(analyticsService.getChapterPerformances(userId, subjectId));
    }

    @GetMapping("/chapters/{chapterId}")
    public ResponseEntity<ChapterAnalyticsDetailResponse> getChapterDetail(
            @PathVariable Long chapterId,
            Authentication authentication
    ) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(analyticsService.getChapterDetail(userId, chapterId));
    }

    @GetMapping("/weak-areas")
    public ResponseEntity<WeakAreasResponse> getWeakAreas(Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(analyticsService.getWeakAreas(userId));
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

