package com.allearning.backend.learning.controller;

import com.allearning.backend.learning.dto.AttemptDetailResponse;
import com.allearning.backend.learning.dto.MistakeItemResponse;
import com.allearning.backend.learning.dto.MistakePracticeRequest;
import com.allearning.backend.learning.dto.MistakeSummaryResponse;
import com.allearning.backend.learning.service.LearningUserLookupService;
import com.allearning.backend.learning.service.MistakeService;
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
@RequestMapping("/api/me/mistakes")
public class MistakeController {

    private final MistakeService mistakeService;
    private final LearningUserLookupService userLookupService;

    public MistakeController(
            MistakeService mistakeService,
            LearningUserLookupService userLookupService
    ) {
        this.mistakeService = mistakeService;
        this.userLookupService = userLookupService;
    }

    @GetMapping
    public ResponseEntity<Page<MistakeItemResponse>> getMistakes(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long chapterId,
            @PageableDefault(size = 20) @SortDefault(sort = "lastAnsweredAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication
    ) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(mistakeService.getMistakes(userId, status, subjectId, chapterId, pageable));
    }

    @GetMapping("/summary")
    public ResponseEntity<MistakeSummaryResponse> getMistakesSummary(Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(mistakeService.getMistakesSummary(userId));
    }

    @PostMapping("/practice")
    public ResponseEntity<AttemptDetailResponse> practiceMistakes(
            @RequestBody(required = false) MistakePracticeRequest request,
            Authentication authentication
    ) {
        Long userId = extractUserId(authentication);
        AttemptDetailResponse response = mistakeService.createMistakePractice(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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

