package com.allearning.backend.communication.controller;

import com.allearning.backend.communication.dto.*;
import com.allearning.backend.communication.entity.AnnouncementStatus;
import com.allearning.backend.communication.service.AnnouncementService;
import com.allearning.backend.communication.service.UserLookupService;
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
@RequestMapping("/api/admin/announcements")
public class AdminAnnouncementController {

    private final AnnouncementService announcementService;
    private final UserLookupService userLookupService;

    public AdminAnnouncementController(
            AnnouncementService announcementService,
            UserLookupService userLookupService
    ) {
        this.announcementService = announcementService;
        this.userLookupService = userLookupService;
    }

    /**
     * GET /api/admin/announcements
     * Returns paginated announcements with optional status filter. Includes creator's name.
     */
    @GetMapping
    public ResponseEntity<Page<AdminAnnouncementResponse>> getAdminAnnouncements(
            @RequestParam(required = false) AnnouncementStatus status,
            @PageableDefault(size = 20) @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(announcementService.getAdminAnnouncements(status, pageable));
    }

    /**
     * GET /api/admin/announcements/{id}
     * Returns full admin view of the announcement.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AdminAnnouncementResponse> getAdminAnnouncement(@PathVariable Long id) {
        return ResponseEntity.ok(announcementService.getAdminAnnouncement(id));
    }

    /**
     * POST /api/admin/announcements
     * Creates an announcement in DRAFT status. created_by is always set from logged-in admin.
     */
    @PostMapping
    public ResponseEntity<AdminAnnouncementResponse> createAnnouncement(
            @Valid @RequestBody AnnouncementCreateRequest request,
            Authentication authentication
    ) {
        Long adminUserId = extractAdminUserId(authentication);
        AdminAnnouncementResponse response = announcementService.createAnnouncement(request, adminUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PATCH /api/admin/announcements/{id}
     * Edits title and content (allowed in any status).
     */
    @PatchMapping("/{id}")
    public ResponseEntity<AdminAnnouncementResponse> updateAnnouncement(
            @PathVariable Long id,
            @Valid @RequestBody AnnouncementUpdateRequest request
    ) {
        return ResponseEntity.ok(announcementService.updateAnnouncement(id, request));
    }

    /**
     * PATCH /api/admin/announcements/{id}/status
     * Transitions status per allowed state machine rules.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminAnnouncementResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody AnnouncementStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(announcementService.updateStatus(id, request.status()));
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
