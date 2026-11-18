package com.allearning.backend.learning.controller;

import com.allearning.backend.learning.dto.StudySessionCreateRequest;
import com.allearning.backend.learning.dto.StudySessionItemResponse;
import com.allearning.backend.learning.dto.StudySessionResponse;
import com.allearning.backend.learning.dto.StudySessionSummaryDto;
import com.allearning.backend.learning.entity.StudySessionStatus;
import com.allearning.backend.learning.service.LearningUserLookupService;
import com.allearning.backend.learning.service.StudySessionService;
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
@RequestMapping("/api/me/study-sessions")
public class StudySessionController {

    private final StudySessionService studySessionService;
    private final LearningUserLookupService userLookupService;

    public StudySessionController(
            StudySessionService studySessionService,
            LearningUserLookupService userLookupService
    ) {
        this.studySessionService = studySessionService;
        this.userLookupService = userLookupService;
    }

    @PostMapping
    public ResponseEntity<StudySessionResponse> createSession(
            @Valid @RequestBody StudySessionCreateRequest request,
            Authentication authentication
    ) {
        Long userId = extractUserId(authentication);
        StudySessionResponse response = studySessionService.startOrCreateSession(userId, request);
        if (Boolean.TRUE.equals(response.resumed())) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudySessionResponse> getSession(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(studySessionService.getSession(userId, id));
    }

    @GetMapping
    public ResponseEntity<Page<StudySessionSummaryDto>> getMySessions(
            @RequestParam(required = false) StudySessionStatus status,
            @PageableDefault(size = 20) @SortDefault(sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication
    ) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(studySessionService.getMyStudySessions(userId, status, pageable));
    }

    @PatchMapping("/{id}/items/{itemId}/complete")
    public ResponseEntity<StudySessionItemResponse> completeResourceItem(
            @PathVariable Long id,
            @PathVariable Long itemId,
            Authentication authentication
    ) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(studySessionService.completeResourceItem(userId, id, itemId));
    }

    @PostMapping("/{id}/abandon")
    public ResponseEntity<StudySessionResponse> abandonSession(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(studySessionService.abandonSession(userId, id));
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

