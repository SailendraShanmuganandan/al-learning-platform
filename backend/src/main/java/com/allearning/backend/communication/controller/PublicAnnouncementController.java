package com.allearning.backend.communication.controller;

import com.allearning.backend.communication.dto.AnnouncementDetailResponse;
import com.allearning.backend.communication.dto.AnnouncementListItemResponse;
import com.allearning.backend.communication.service.AnnouncementService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/announcements")
public class PublicAnnouncementController {

    private final AnnouncementService announcementService;

    public PublicAnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    /**
     * GET /api/announcements
     * Returns published announcements ordered by published_at DESC.
     */
    @GetMapping
    public ResponseEntity<Page<AnnouncementListItemResponse>> getPublishedAnnouncements(
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(announcementService.getPublishedAnnouncements(pageable));
    }

    /**
     * GET /api/announcements/{id}
     * Returns full content for published announcement; 404 if not found or not PUBLISHED.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AnnouncementDetailResponse> getPublishedAnnouncement(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(announcementService.getPublishedAnnouncement(id));
    }
}
