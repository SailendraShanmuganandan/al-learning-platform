package com.allearning.backend.communication.service;

import com.allearning.backend.communication.dto.*;
import com.allearning.backend.communication.entity.AnnouncementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AnnouncementService {

    // --- Public ---
    Page<AnnouncementListItemResponse> getPublishedAnnouncements(Pageable pageable);

    AnnouncementDetailResponse getPublishedAnnouncement(Long id);

    // --- Admin ---
    Page<AdminAnnouncementResponse> getAdminAnnouncements(AnnouncementStatus status, Pageable pageable);

    AdminAnnouncementResponse getAdminAnnouncement(Long id);

    AdminAnnouncementResponse createAnnouncement(AnnouncementCreateRequest request, Long adminUserId);

    AdminAnnouncementResponse updateAnnouncement(Long id, AnnouncementUpdateRequest request);

    AdminAnnouncementResponse updateStatus(Long id, AnnouncementStatus newStatus);
}
