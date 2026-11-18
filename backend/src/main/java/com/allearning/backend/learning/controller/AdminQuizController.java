package com.allearning.backend.learning.controller;

import com.allearning.backend.learning.dto.QuizDetailResponse;
import com.allearning.backend.learning.dto.QuizStatusUpdateRequest;
import com.allearning.backend.learning.dto.QuizSummaryResponse;
import com.allearning.backend.learning.dto.QuizUpdateRequest;
import com.allearning.backend.learning.entity.QuizStatus;
import com.allearning.backend.learning.service.QuizService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/quizzes")
public class AdminQuizController {

    private final QuizService quizService;

    public AdminQuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping
    public ResponseEntity<Page<QuizSummaryResponse>> getAdminQuizzes(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long chapterId,
            @RequestParam(required = false) QuizStatus status,
            @PageableDefault(size = 20) @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(quizService.getAdminQuizzes(subjectId, chapterId, status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuizDetailResponse> getQuizDetail(@PathVariable Long id) {
        return ResponseEntity.ok(quizService.getQuizDetail(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<QuizDetailResponse> updateQuiz(
            @PathVariable Long id,
            @Valid @RequestBody QuizUpdateRequest request
    ) {
        return ResponseEntity.ok(quizService.updateQuiz(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<QuizDetailResponse> updateQuizStatus(
            @PathVariable Long id,
            @Valid @RequestBody QuizStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(quizService.updateQuizStatus(id, request));
    }
}

