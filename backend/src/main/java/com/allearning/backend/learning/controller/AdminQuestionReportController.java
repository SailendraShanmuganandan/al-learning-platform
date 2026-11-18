package com.allearning.backend.learning.controller;

import com.allearning.backend.learning.dto.QuestionReportAdminResponse;
import com.allearning.backend.learning.dto.QuestionReportStatusUpdateRequest;
import com.allearning.backend.learning.entity.QuestionReportStatus;
import com.allearning.backend.learning.service.LearningUserLookupService;
import com.allearning.backend.learning.service.QuestionReportService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/question-reports")
@PreAuthorize("hasRole('ADMIN')")
public class AdminQuestionReportController {

    private final QuestionReportService questionReportService;
    private final LearningUserLookupService userLookupService;

    public AdminQuestionReportController(
            QuestionReportService questionReportService,
            LearningUserLookupService userLookupService
    ) {
        this.questionReportService = questionReportService;
        this.userLookupService = userLookupService;
    }

    @GetMapping
    public ResponseEntity<Page<QuestionReportAdminResponse>> getReports(
            @RequestParam(required = false) QuestionReportStatus status,
            @RequestParam(required = false) Long questionId,
            @RequestParam(required = false) Long chapterId,
            @PageableDefault(size = 20) @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(questionReportService.getAdminReports(status, questionId, chapterId, pageable));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<QuestionReportAdminResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody QuestionReportStatusUpdateRequest request,
            Authentication authentication
    ) {
        Long adminId = extractAdminId(authentication);
        return ResponseEntity.ok(questionReportService.updateReportStatus(id, adminId, request.status()));
    }

    private Long extractAdminId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return userLookupService.findUserIdByEmail(authentication.getName());
    }
}

