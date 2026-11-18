package com.allearning.backend.communication.dto;

public record UnreadCountResponse(
        long unreadCount,
        long count
) {
    public UnreadCountResponse(long count) {
        this(count, count);
    }
}
