package com.allearning.backend.learning.controller;

import com.allearning.backend.learning.dto.*;
import com.allearning.backend.learning.entity.QuestionDifficulty;
import com.allearning.backend.learning.entity.QuestionSource;
import com.allearning.backend.learning.entity.QuestionStatus;
import com.allearning.backend.learning.service.LearningUserLookupService;
import com.allearning.backend.learning.service.QuestionService;
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

@RestController
@RequestMapping("/api/admin")
public class AdminQuestionController {

    private final QuestionService questionService;
    private final LearningUserLookupService userLookupService;

    public AdminQuestionController(
            QuestionService questionService,
            LearningUserLookupService userLookupService
    ) {
        this.questionService = questionService;
        this.userLookupService = userLookupService;
    }

    @GetMapping("/questions")
    public ResponseEntity<Page<QuestionSummaryResponse>> getAllQuestions(
            @RequestParam(required = false) Long chapterId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long quizId,
            @RequestParam(required = false) Long topicId,
            @RequestParam(required = false) QuestionStatus status,
            @RequestParam(required = false) QuestionDifficulty difficulty,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) QuestionSource source,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean reported,
            @RequestParam(required = false) Boolean lowCorrectRate,
            @RequestParam(required = false) Boolean sourceDocumentRemoved,
            @RequestParam(required = false) Long sourceDocumentId,
            @RequestParam(required = false) Long generationJobId,
            @PageableDefault(size = 20) @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(questionService.getQuestions(
                chapterId, subjectId, quizId, topicId, status, difficulty, language, source, search,
                reported, lowCorrectRate, sourceDocumentRemoved, sourceDocumentId, generationJobId, pageable
        ));
    }

    @GetMapping("/chapters/{chapterId}/questions")
    public ResponseEntity<Page<QuestionSummaryResponse>> getQuestions(
            @PathVariable Long chapterId,
            @RequestParam(required = false) Long quizId,
            @RequestParam(required = false) Long topicId,
            @RequestParam(required = false) QuestionStatus status,
            @RequestParam(required = false) QuestionDifficulty difficulty,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) QuestionSource source,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean reported,
            @RequestParam(required = false) Boolean lowCorrectRate,
            @RequestParam(required = false) Boolean sourceDocumentRemoved,
            @RequestParam(required = false) Long sourceDocumentId,
            @RequestParam(required = false) Long generationJobId,
            @PageableDefault(size = 20) @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(questionService.getQuestions(
                chapterId, quizId, topicId, status, difficulty, language, source, search,
                reported, lowCorrectRate, sourceDocumentRemoved, sourceDocumentId, generationJobId, pageable
        ));
    }

    @GetMapping("/questions/{id}")
    public ResponseEntity<QuestionDetailResponse> getQuestionDetail(@PathVariable Long id) {
        return ResponseEntity.ok(questionService.getQuestionDetail(id));
    }

    @PostMapping("/chapters/{chapterId}/questions")
    public ResponseEntity<QuestionDetailResponse> createQuestion(
            @PathVariable Long chapterId,
            @Valid @RequestBody QuestionCreateRequest request,
            Authentication authentication
    ) {
        Long adminUserId = extractAdminUserId(authentication);
        QuestionDetailResponse response = questionService.createQuestion(chapterId, request, adminUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/questions/{id}")
    public ResponseEntity<QuestionDetailResponse> updateQuestion(
            @PathVariable Long id,
            @Valid @RequestBody QuestionUpdateRequest request,
            Authentication authentication
    ) {
        Long adminUserId = extractAdminUserId(authentication);
        return ResponseEntity.ok(questionService.updateQuestion(id, request, adminUserId));
    }

    @PatchMapping("/questions/{id}/status")
    public ResponseEntity<QuestionDetailResponse> updateQuestionStatus(
            @PathVariable Long id,
            @Valid @RequestBody QuestionStatusUpdateRequest request,
            Authentication authentication
    ) {
        Long adminUserId = extractAdminUserId(authentication);
        return ResponseEntity.ok(questionService.updateQuestionStatus(id, request.status(), adminUserId));
    }

    @PostMapping("/questions/bulk-status")
    public ResponseEntity<BulkStatusResponse> bulkUpdateStatus(
            @Valid @RequestBody BulkStatusRequest request,
            Authentication authentication
    ) {
        Long adminUserId = extractAdminUserId(authentication);
        return ResponseEntity.ok(questionService.bulkUpdateStatus(request, adminUserId));
    }

    private Long extractAdminUserId(Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        String email = authentication.getName();
        Long userId = userLookupService.findUserIdByEmail(email);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin user not found");
        }
        return userId;
    }
}

