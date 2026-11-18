package com.allearning.backend.communication.service;

import com.allearning.backend.communication.dto.*;
import com.allearning.backend.communication.entity.ContactMessage;
import com.allearning.backend.communication.entity.ContactMessageStatus;
import com.allearning.backend.communication.repository.ContactMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.regex.Pattern;

@Service
@Transactional
public class ContactMessageServiceImpl implements ContactMessageService {

    private static final Logger log = LoggerFactory.getLogger(ContactMessageServiceImpl.class);

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+[1-9]\\d{6,14}$");

    private final ContactMessageRepository contactMessageRepository;
    private final ContactRateLimiter rateLimiter;

    public ContactMessageServiceImpl(
            ContactMessageRepository contactMessageRepository,
            ContactRateLimiter rateLimiter
    ) {
        this.contactMessageRepository = contactMessageRepository;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public ContactSuccessResponse submitContact(ContactMessageRequest request, String clientIp) {
        // 1. Rate limiting (SEC-13)
        rateLimiter.checkRateLimit(clientIp);

        // 2. Honeypot check: if website is filled, silently return success without saving
        if (request.website() != null && !request.website().trim().isEmpty()) {
            log.info("Honeypot triggered for submission from IP: {}", clientIp);
            return new ContactSuccessResponse(true, "Thank you for reaching out. Your message has been received.");
        }

        // 3. Validation
        validateContactRequest(request);

        // 4. Save to database (never linked to a user)
        String trimmedName = request.name().trim();
        String trimmedEmail = request.email().trim().toLowerCase();
        String trimmedPhone = (request.phone() != null && !request.phone().trim().isEmpty())
                ? request.phone().trim()
                : null;
        String trimmedMessage = request.message().trim();

        ContactMessage message = new ContactMessage(
                trimmedName,
                trimmedEmail,
                trimmedPhone,
                trimmedMessage
        );
        contactMessageRepository.save(message);

        return new ContactSuccessResponse(true, "Thank you for reaching out. Your message has been received.");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminContactMessageListItem> getContactMessages(ContactMessageStatus status, String q, Pageable pageable) {
        // Enforce newest created_at first
        Pageable sortedByDate = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        String searchQuery = (q != null && !q.trim().isEmpty()) ? q.trim() : null;

        Page<ContactMessage> page = contactMessageRepository.findMessages(status, searchQuery, sortedByDate);

        return page.map(m -> new AdminContactMessageListItem(
                m.getMessageId(),
                m.getName(),
                m.getEmail(),
                m.getPhone(),
                extractExcerpt(m.getMessage()),
                m.getStatus(),
                m.getCreatedAt()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminContactMessageDetail getContactMessage(Long id) {
        // Reading does NOT change the status
        ContactMessage m = contactMessageRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact message not found"));

        return toDetail(m);
    }

    @Override
    public AdminContactMessageDetail updateStatus(Long id, ContactMessageStatus newStatus) {
        ContactMessage m = contactMessageRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact message not found"));

        if (newStatus == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status cannot be null");
        }

        // Setting the same status again is a no-op returning 200
        if (m.getStatus() != newStatus) {
            m.setStatus(newStatus);
            m = contactMessageRepository.save(m);
        }

        return toDetail(m);
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount() {
        long count = contactMessageRepository.countByStatus(ContactMessageStatus.UNREAD);
        return new UnreadCountResponse(count);
    }

    private void validateContactRequest(ContactMessageRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }

        // Name: 2-100 characters
        if (request.name() == null || request.name().trim().length() < 2 || request.name().trim().length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name must be between 2 and 100 characters");
        }

        // Email: valid email, max 254
        if (request.email() == null || request.email().trim().isEmpty() || request.email().trim().length() > 254
                || !EMAIL_PATTERN.matcher(request.email().trim()).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid email address is required (max 254 characters)");
        }

        // Phone: optional, E.164 if provided
        if (request.phone() != null && !request.phone().trim().isEmpty()) {
            if (!PHONE_PATTERN.matcher(request.phone().trim()).matches() || request.phone().trim().length() > 30) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone must be in E.164 format (e.g. +911234567890)");
            }
        }

        // Message: 10-2000 characters
        if (request.message() == null || request.message().trim().length() < 10 || request.message().trim().length() > 2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message must be between 10 and 2000 characters");
        }
    }

    private String extractExcerpt(String content) {
        if (content == null) return "";
        if (content.length() <= 120) {
            return content;
        }
        return content.substring(0, 120) + "…";
    }

    private AdminContactMessageDetail toDetail(ContactMessage m) {
        return new AdminContactMessageDetail(
                m.getMessageId(),
                m.getName(),
                m.getEmail(),
                m.getPhone(),
                m.getMessage(),
                m.getStatus(),
                m.getCreatedAt(),
                m.getUpdatedAt()
        );
    }
}
