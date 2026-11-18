package com.allearning.backend.communication.service;

import com.allearning.backend.communication.dto.CommunicationStatisticsDto;
import com.allearning.backend.communication.dto.RecentContactMessageDto;
import com.allearning.backend.communication.entity.AnnouncementStatus;
import com.allearning.backend.communication.entity.ContactMessageStatus;
import com.allearning.backend.communication.repository.AnnouncementRepository;
import com.allearning.backend.communication.repository.ContactMessageRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CommunicationStatisticsServiceImpl implements CommunicationStatisticsService {

    private final AnnouncementRepository announcementRepository;
    private final ContactMessageRepository contactMessageRepository;

    public CommunicationStatisticsServiceImpl(
            AnnouncementRepository announcementRepository,
            ContactMessageRepository contactMessageRepository
    ) {
        this.announcementRepository = announcementRepository;
        this.contactMessageRepository = contactMessageRepository;
    }

    @Override
    public CommunicationStatisticsDto getCommunicationStatistics() {
        long unreadMessages = contactMessageRepository.countByStatus(ContactMessageStatus.UNREAD);
        long publishedAnnouncements = announcementRepository.countByStatus(AnnouncementStatus.PUBLISHED);
        long draftAnnouncements = announcementRepository.countByStatus(AnnouncementStatus.DRAFT);

        List<RecentContactMessageDto> recentMessages = contactMessageRepository.findRecentMessages(PageRequest.of(0, 5));

        return new CommunicationStatisticsDto(unreadMessages, publishedAnnouncements, draftAnnouncements, recentMessages);
    }
}
