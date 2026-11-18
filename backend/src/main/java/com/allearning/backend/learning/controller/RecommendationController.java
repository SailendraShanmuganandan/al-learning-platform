package com.allearning.backend.learning.controller;

import com.allearning.backend.learning.dto.LearningOverviewResponse;
import com.allearning.backend.learning.dto.Recommendation;
import com.allearning.backend.learning.service.LearningUserLookupService;
import com.allearning.backend.learning.service.RecommendationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/me")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final LearningUserLookupService userLookupService;

    public RecommendationController(
            RecommendationService recommendationService,
            LearningUserLookupService userLookupService
    ) {
        this.recommendationService = recommendationService;
        this.userLookupService = userLookupService;
    }

    @GetMapping("/recommendations/next")
    public ResponseEntity<Recommendation> getNextRecommendation(Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(recommendationService.getNextRecommendation(userId));
    }

    @GetMapping("/learning/overview")
    public ResponseEntity<LearningOverviewResponse> getLearningOverview(Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(recommendationService.getOverview(userId));
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

