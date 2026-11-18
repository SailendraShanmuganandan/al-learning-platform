package com.allearning.backend.admin.dto;

import com.allearning.backend.catalog.dto.LowStockItemDto;
import com.allearning.backend.classes.dto.RecentRegistrationDto;
import com.allearning.backend.commerce.dto.RecentOrderDto;
import com.allearning.backend.communication.dto.RecentContactMessageDto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record DashboardSummaryResponse(
        UsersSectionDto users,
        SalesSectionDto sales,
        PendingActionsSectionDto pendingActions,
        CatalogSectionDto catalog,
        ClassesSectionDto classes,
        CommunicationSectionDto communication,
        RecentActivitiesSectionDto recent,
        Map<String, String> sectionErrors,
        OffsetDateTime generatedAt
) {
    public record UsersSectionDto(
            long totalStudents,
            long newStudentsLast30Days,
            long unverifiedStudents
    ) {}

    public record SalesSectionDto(
            BigDecimal revenueToday,
            BigDecimal revenueLast7Days,
            BigDecimal revenueLast30Days,
            BigDecimal revenueAllTime,
            long paidOrdersCount,
            long awaitingPaymentCount
    ) {}

    public record PendingActionsSectionDto(
            long ordersAwaitingFulfillment,
            long paymentsNeedingReview,
            long pendingClassRegistrations,
            long unreadContactMessages,
            long pendingQuestions,
            long openQuestionReports,
            long failedGenerationJobs
    ) {
        public PendingActionsSectionDto(
                long ordersAwaitingFulfillment,
                long paymentsNeedingReview,
                long pendingClassRegistrations,
                long unreadContactMessages
        ) {
            this(ordersAwaitingFulfillment, paymentsNeedingReview, pendingClassRegistrations, unreadContactMessages, 0L, 0L, 0L);
        }
    }

    public record CatalogSectionDto(
            long publishedNotes,
            long publishedVideos,
            long draftProducts,
            long lowStockCount,
            List<LowStockItemDto> lowStockItems
    ) {}

    public record ClassesSectionDto(
            long activeClasses,
            long pendingRegistrations,
            long confirmedRegistrations
    ) {}

    public record CommunicationSectionDto(
            long publishedAnnouncements,
            long draftAnnouncements
    ) {}

    public record RecentActivitiesSectionDto(
            List<RecentOrderDto> orders,
            List<RecentRegistrationDto> classRegistrations,
            List<RecentContactMessageDto> contactMessages
    ) {}
}
