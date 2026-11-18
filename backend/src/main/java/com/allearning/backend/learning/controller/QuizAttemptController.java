package com.allearning.backend.learning.controller;

import com.allearning.backend.learning.dto.AttemptDetailResponse;
import com.allearning.backend.learning.dto.SubmitAnswersRequest;
import com.allearning.backend.learning.service.LearningUserLookupService;
import com.allearning.backend.learning.service.QuizAttemptService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/quiz-attempts")
public class QuizAttemptController {

    private final QuizAttemptService quizAttemptService;
    private final LearningUserLookupService userLookupService;

    public QuizAttemptController(
            QuizAttemptService quizAttemptService,
            LearningUserLookupService userLookupService
    ) {
        this.quizAttemptService = quizAttemptService;
        this.userLookupService = userLookupService;
    }

    @GetMapping("/{attemptId}")
    public ResponseEntity<AttemptDetailResponse> getAttempt(
            @PathVariable Long attemptId,
            Authentication authentication
    ) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(quizAttemptService.getAttemptDetail(userId, attemptId));
    }

    @PostMapping("/{attemptId}/submit")
    public ResponseEntity<AttemptDetailResponse> submitAttempt(
            @PathVariable Long attemptId,
            @Valid @RequestBody SubmitAnswersRequest request,
            Authentication authentication
    ) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(quizAttemptService.submitAttempt(userId, attemptId, request));
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

