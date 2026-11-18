package com.allearning.backend.communication.service;

import com.allearning.backend.communication.dto.*;
import com.allearning.backend.communication.entity.ContactMessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ContactMessageService {

    // --- Public ---
    ContactSuccessResponse submitContact(ContactMessageRequest request, String clientIp);

    // --- Admin ---
    Page<AdminContactMessageListItem> getContactMessages(ContactMessageStatus status, String q, Pageable pageable);

    AdminContactMessageDetail getContactMessage(Long id);

    AdminContactMessageDetail updateStatus(Long id, ContactMessageStatus status);

    UnreadCountResponse getUnreadCount();
}
