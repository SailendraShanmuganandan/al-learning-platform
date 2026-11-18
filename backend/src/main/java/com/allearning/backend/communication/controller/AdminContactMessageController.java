package com.allearning.backend.communication.controller;

import com.allearning.backend.communication.dto.*;
import com.allearning.backend.communication.entity.ContactMessageStatus;
import com.allearning.backend.communication.service.ContactMessageService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/contact-messages")
public class AdminContactMessageController {

    private final ContactMessageService contactMessageService;

    public AdminContactMessageController(ContactMessageService contactMessageService) {
        this.contactMessageService = contactMessageService;
    }

    /**
     * GET /api/admin/contact-messages
     * Lists contact messages with optional status and q (name/email) filter.
     */
    @GetMapping
    public ResponseEntity<Page<AdminContactMessageListItem>> getContactMessages(
            @RequestParam(required = false) ContactMessageStatus status,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(contactMessageService.getContactMessages(status, q, pageable));
    }

    /**
     * GET /api/admin/contact-messages/{id}
     * Full message details. Reading does NOT change status.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AdminContactMessageDetail> getContactMessage(@PathVariable Long id) {
        return ResponseEntity.ok(contactMessageService.getContactMessage(id));
    }

    /**
     * PATCH /api/admin/contact-messages/{id}/status
     * Updates status to UNREAD, READ, RESPONDED, or ARCHIVED. Setting same status is a no-op returning 200.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminContactMessageDetail> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ContactMessageStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(contactMessageService.updateStatus(id, request.status()));
    }

    /**
     * GET /api/admin/contact-messages/unread-count
     * Returns count of UNREAD messages for navigation badges.
     */
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount() {
        return ResponseEntity.ok(contactMessageService.getUnreadCount());
    }
}
