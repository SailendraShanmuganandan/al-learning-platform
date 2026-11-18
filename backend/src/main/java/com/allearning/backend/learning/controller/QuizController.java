package com.allearning.backend.learning.controller;

import com.allearning.backend.learning.dto.*;
import com.allearning.backend.learning.service.LearningUserLookupService;
import com.allearning.backend.learning.service.QuizAttemptService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class QuizController {

    private final QuizAttemptService quizAttemptService;
    private final LearningUserLookupService userLookupService;

    public QuizController(
            QuizAttemptService quizAttemptService,
            LearningUserLookupService userLookupService
    ) {
        this.quizAttemptService = quizAttemptService;
        this.userLookupService = userLookupService;
    }

    @GetMapping("/quizzes/{quizId}")
    public ResponseEntity<QuizAvailabilityResponse> getQuizAvailability(@PathVariable Long quizId) {
        return ResponseEntity.ok(quizAttemptService.getQuizAvailability(quizId));
    }

    @GetMapping("/chapters/{chapterId}/quiz")
    public ResponseEntity<QuizAvailabilityResponse> getChapterQuiz(@PathVariable Long chapterId) {
        return ResponseEntity.ok(quizAttemptService.getChapterQuizAvailability(chapterId));
    }

    @PostMapping("/quizzes/{quizId}/attempts")
    public ResponseEntity<AttemptDetailResponse> startAttempt(
            @PathVariable Long quizId,
            @RequestBody(required = false) AttemptStartRequest request,
            Authentication authentication
    ) {
        Long userId = extractUserId(authentication);
        String lang = (request != null) ? request.language() : null;
        AttemptDetailResponse response = quizAttemptService.startChapterQuizAttempt(userId, quizId, lang);
        HttpStatus status = Boolean.TRUE.equals(response.resumed()) ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/quizzes/{quizId}/my-attempts")
    public ResponseEntity<MyAttemptsResponse> getMyAttempts(
            @PathVariable Long quizId,
            @PageableDefault(size = 10) @SortDefault(sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication
    ) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(quizAttemptService.getMyAttempts(userId, quizId, pageable));
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

