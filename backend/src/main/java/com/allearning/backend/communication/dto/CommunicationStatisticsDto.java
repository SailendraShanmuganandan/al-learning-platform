package com.allearning.backend.communication.dto;

import java.util.List;

public record CommunicationStatisticsDto(
        long unreadMessages,
        long publishedAnnouncements,
        long draftAnnouncements,
        List<RecentContactMessageDto> recentMessages
) {
}
