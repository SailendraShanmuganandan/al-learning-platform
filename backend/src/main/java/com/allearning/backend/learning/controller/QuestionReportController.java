package com.allearning.backend.learning.controller;

import com.allearning.backend.learning.dto.QuestionReportCreateRequest;
import com.allearning.backend.learning.dto.QuestionReportCreateResponse;
import com.allearning.backend.learning.service.LearningUserLookupService;
import com.allearning.backend.learning.service.QuestionReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/questions/{questionId}/reports")
public class QuestionReportController {

    private final QuestionReportService questionReportService;
    private final LearningUserLookupService userLookupService;

    public QuestionReportController(
            QuestionReportService questionReportService,
            LearningUserLookupService userLookupService
    ) {
        this.questionReportService = questionReportService;
        this.userLookupService = userLookupService;
    }

    @PostMapping
    public ResponseEntity<QuestionReportCreateResponse> createReport(
            @PathVariable Long questionId,
            @Valid @RequestBody QuestionReportCreateRequest request,
            Authentication authentication
    ) {
        Long userId = extractUserId(authentication);
        QuestionReportCreateResponse response = questionReportService.createReport(userId, questionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return userLookupService.findUserIdByEmail(authentication.getName());
    }
}

