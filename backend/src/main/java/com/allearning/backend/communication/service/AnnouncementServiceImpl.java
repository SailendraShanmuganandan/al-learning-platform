package com.allearning.backend.communication.service;

import com.allearning.backend.communication.dto.*;
import com.allearning.backend.communication.entity.Announcement;
import com.allearning.backend.communication.entity.AnnouncementStatus;
import com.allearning.backend.communication.exception.InvalidTransitionException;
import com.allearning.backend.communication.repository.AnnouncementRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final UserLookupService userLookupService;

    public AnnouncementServiceImpl(
            AnnouncementRepository announcementRepository,
            UserLookupService userLookupService
    ) {
        this.announcementRepository = announcementRepository;
        this.userLookupService = userLookupService;
    }

    // --- Public methods ---

    @Override
    @Transactional(readOnly = true)
    public Page<AnnouncementListItemResponse> getPublishedAnnouncements(Pageable pageable) {
        // Enforce newest published_at first
        Pageable sortedByPublished = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "publishedAt")
        );

        Page<Announcement> page = announcementRepository.findByStatus(
                AnnouncementStatus.PUBLISHED,
                sortedByPublished
        );

        return page.map(a -> new AnnouncementListItemResponse(
                a.getAnnouncementId(),
                a.getTitle(),
                extractExcerpt(a.getContent()),
                a.getPublishedAt()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public AnnouncementDetailResponse getPublishedAnnouncement(Long id) {
        Announcement a = announcementRepository.findByAnnouncementIdAndStatus(id, AnnouncementStatus.PUBLISHED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Announcement not found"));

        return new AnnouncementDetailResponse(
                a.getAnnouncementId(),
                a.getTitle(),
                a.getContent(),
                a.getPublishedAt()
        );
    }

    // --- Admin methods ---

    @Override
    @Transactional(readOnly = true)
    public Page<AdminAnnouncementResponse> getAdminAnnouncements(AnnouncementStatus status, Pageable pageable) {
        Page<Announcement> page;
        if (status != null) {
            page = announcementRepository.findByStatus(status, pageable);
        } else {
            page = announcementRepository.findAll(pageable);
        }

        Set<Long> userIds = page.getContent().stream()
                .map(Announcement::getCreatedBy)
                .collect(Collectors.toSet());

        Map<Long, String> nameMap = userLookupService.getUserDisplayNames(userIds);

        return page.map(a -> toAdminResponse(a, nameMap.get(a.getCreatedBy())));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminAnnouncementResponse getAdminAnnouncement(Long id) {
        Announcement a = announcementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Announcement not found"));

        Map<Long, String> nameMap = userLookupService.getUserDisplayNames(Set.of(a.getCreatedBy()));
        return toAdminResponse(a, nameMap.get(a.getCreatedBy()));
    }

    @Override
    public AdminAnnouncementResponse createAnnouncement(AnnouncementCreateRequest request, Long adminUserId) {
        Announcement announcement = new Announcement();
        announcement.setTitle(request.title().trim());
        announcement.setContent(request.content().trim());
        announcement.setCreatedBy(adminUserId);
        announcement.setStatus(AnnouncementStatus.DRAFT);
        announcement.setPublishedAt(null);

        Announcement saved = announcementRepository.save(announcement);

        Map<Long, String> nameMap = userLookupService.getUserDisplayNames(Set.of(adminUserId));
        return toAdminResponse(saved, nameMap.get(adminUserId));
    }

    @Override
    public AdminAnnouncementResponse updateAnnouncement(Long id, AnnouncementUpdateRequest request) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Announcement not found"));

        // Allowed in any status; do not change published_at or created_by
        announcement.setTitle(request.title().trim());
        announcement.setContent(request.content().trim());

        Announcement updated = announcementRepository.save(announcement);

        Map<Long, String> nameMap = userLookupService.getUserDisplayNames(Set.of(updated.getCreatedBy()));
        return toAdminResponse(updated, nameMap.get(updated.getCreatedBy()));
    }

    @Override
    public AdminAnnouncementResponse updateStatus(Long id, AnnouncementStatus newStatus) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Announcement not found"));

        AnnouncementStatus currentStatus = announcement.getStatus();
        validateTransition(currentStatus, newStatus);

        announcement.setStatus(newStatus);
        if (newStatus == AnnouncementStatus.PUBLISHED) {
            announcement.setPublishedAt(OffsetDateTime.now());
        }

        Announcement updated = announcementRepository.save(announcement);

        Map<Long, String> nameMap = userLookupService.getUserDisplayNames(Set.of(updated.getCreatedBy()));
        return toAdminResponse(updated, nameMap.get(updated.getCreatedBy()));
    }

    private void validateTransition(AnnouncementStatus current, AnnouncementStatus target) {
        if (current == target) {
            throw new InvalidTransitionException(
                    "INVALID_TRANSITION",
                    "Announcement is already in status " + current
            );
        }

        boolean allowed = switch (current) {
            case DRAFT -> target == AnnouncementStatus.PUBLISHED || target == AnnouncementStatus.ARCHIVED;
            case PUBLISHED -> target == AnnouncementStatus.ARCHIVED;
            case ARCHIVED -> target == AnnouncementStatus.DRAFT;
        };

        if (!allowed) {
            throw new InvalidTransitionException(
                    "INVALID_TRANSITION",
                    String.format("Invalid announcement status transition from %s to %s", current, target)
            );
        }
    }

    private String extractExcerpt(String content) {
        if (content == null) return "";
        if (content.length() <= 200) {
            return content;
        }
        return content.substring(0, 200);
    }

    private AdminAnnouncementResponse toAdminResponse(Announcement a, String createdByName) {
        return new AdminAnnouncementResponse(
                a.getAnnouncementId(),
                a.getTitle(),
                a.getContent(),
                a.getStatus(),
                a.getCreatedBy(),
                createdByName != null ? createdByName : "Admin #" + a.getCreatedBy(),
                a.getPublishedAt(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}
