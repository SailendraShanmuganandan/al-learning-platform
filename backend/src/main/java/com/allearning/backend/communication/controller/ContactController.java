package com.allearning.backend.communication.controller;

import com.allearning.backend.communication.dto.ContactMessageRequest;
import com.allearning.backend.communication.dto.ContactSuccessResponse;
import com.allearning.backend.communication.service.ContactMessageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contact")
public class ContactController {

    private final ContactMessageService contactMessageService;

    public ContactController(ContactMessageService contactMessageService) {
        this.contactMessageService = contactMessageService;
    }

    /**
     * POST /api/contact
     * Accepts public contact form submissions.
     * Works with or without token, ignores user token, enforces rate limit per IP and honeypot.
     */
    @PostMapping
    public ResponseEntity<ContactSuccessResponse> submitContact(
            @RequestBody ContactMessageRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        ContactSuccessResponse response = contactMessageService.submitContact(request, clientIp);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private String extractClientIp(HttpServletRequest request) {
        // Code comment: Inspect X-Forwarded-For if behind a reverse proxy.
        // Must be revisited for multi-instance deployments and trusted proxy configurations.
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
