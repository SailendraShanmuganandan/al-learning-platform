package com.allearning.backend.admin.service;

import com.allearning.backend.admin.dto.DashboardSummaryResponse;
import com.allearning.backend.admin.dto.DashboardSummaryResponse.*;
import com.allearning.backend.auth.service.UserStatisticsService;
import com.allearning.backend.catalog.dto.CatalogStatisticsDto;
import com.allearning.backend.catalog.service.CatalogStatisticsService;
import com.allearning.backend.classes.dto.ClassStatisticsDto;
import com.allearning.backend.classes.service.ClassStatisticsService;
import com.allearning.backend.commerce.dto.*;
import com.allearning.backend.commerce.service.CommerceStatisticsService;
import com.allearning.backend.communication.dto.CommunicationStatisticsDto;
import com.allearning.backend.communication.service.CommunicationStatisticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardServiceImpl.class);

    private final UserStatisticsService userStatisticsService;
    private final CatalogStatisticsService catalogStatisticsService;
    private final ClassStatisticsService classStatisticsService;
    private final CommunicationStatisticsService communicationStatisticsService;
    private final CommerceStatisticsService commerceStatisticsService;
    private final com.allearning.backend.learning.service.LearningAdminStatsService learningAdminStatsService;
    private final ZoneId configuredZone;
    private final int lowStockThreshold;

    @Autowired
    public DashboardServiceImpl(
            UserStatisticsService userStatisticsService,
            CatalogStatisticsService catalogStatisticsService,
            ClassStatisticsService classStatisticsService,
            CommunicationStatisticsService communicationStatisticsService,
            CommerceStatisticsService commerceStatisticsService,
            @Value("${app.timezone:Asia/Colombo}") String timezoneStr,
            @Value("${app.dashboard.low-stock-threshold:5}") int lowStockThreshold
    ) {
        this(
                userStatisticsService,
                catalogStatisticsService,
                classStatisticsService,
                communicationStatisticsService,
                commerceStatisticsService,
                null,
                timezoneStr,
                lowStockThreshold
        );
    }

    public DashboardServiceImpl(
            UserStatisticsService userStatisticsService,
            CatalogStatisticsService catalogStatisticsService,
            ClassStatisticsService classStatisticsService,
            CommunicationStatisticsService communicationStatisticsService,
            CommerceStatisticsService commerceStatisticsService,
            com.allearning.backend.learning.service.LearningAdminStatsService learningAdminStatsService,
            @Value("${app.timezone:Asia/Colombo}") String timezoneStr,
            @Value("${app.dashboard.low-stock-threshold:5}") int lowStockThreshold
    ) {
        this.userStatisticsService = userStatisticsService;
        this.catalogStatisticsService = catalogStatisticsService;
        this.classStatisticsService = classStatisticsService;
        this.communicationStatisticsService = communicationStatisticsService;
        this.commerceStatisticsService = commerceStatisticsService;
        this.learningAdminStatsService = learningAdminStatsService;
        this.configuredZone = ZoneId.of(timezoneStr);
        this.lowStockThreshold = lowStockThreshold;
    }

    @Override
    public DashboardSummaryResponse getSummary() {
        Map<String, String> sectionErrors = new LinkedHashMap<>();

        // 1. Users
        UsersSectionDto usersSection = null;
        try {
            var stats = userStatisticsService.getUserStatistics(configuredZone);
            usersSection = new UsersSectionDto(stats.totalStudents(), stats.newStudentsLast30Days(), stats.unverifiedStudents());
        } catch (Exception e) {
            log.error("Failed to load users dashboard section", e);
            sectionErrors.put("users", "Failed to load user metrics");
        }

        // 2. Sales
        SalesSectionDto salesSection = null;
        try {
            var stats = commerceStatisticsService.getSalesStatistics(configuredZone);
            salesSection = new SalesSectionDto(
                    stats.revenueToday(),
                    stats.revenueLast7Days(),
                    stats.revenueLast30Days(),
                    stats.revenueAllTime(),
                    stats.paidOrdersCount(),
                    stats.awaitingPaymentCount()
            );
        } catch (Exception e) {
            log.error("Failed to load sales dashboard section", e);
            sectionErrors.put("sales", "Failed to load sales metrics");
        }

        // 3. Classes
        ClassStatisticsDto classStats = null;
        ClassesSectionDto classesSection = null;
        try {
            classStats = classStatisticsService.getClassStatistics();
            classesSection = new ClassesSectionDto(
                    classStats.activeClasses(),
                    classStats.pendingRegistrations(),
                    classStats.confirmedRegistrations()
            );
        } catch (Exception e) {
            log.error("Failed to load classes dashboard section", e);
            sectionErrors.put("classes", "Failed to load class metrics");
        }

        // 4. Communication
        CommunicationStatisticsDto commStats = null;
        CommunicationSectionDto communicationSection = null;
        try {
            commStats = communicationStatisticsService.getCommunicationStatistics();
            communicationSection = new CommunicationSectionDto(
                    commStats.publishedAnnouncements(),
                    commStats.draftAnnouncements()
            );
        } catch (Exception e) {
            log.error("Failed to load communication dashboard section", e);
            sectionErrors.put("communication", "Failed to load communication metrics");
        }

        // 5. Pending Actions (composed of commerce, classes, communication, learning)
        PendingActionsSectionDto pendingActionsSection = null;
        try {
            CommercePendingActionsDto commPending = commerceStatisticsService.getPendingActions();
            long pendingRegs = classStats != null ? classStats.pendingRegistrations() : 0L;
            long unreadMsgs = commStats != null ? commStats.unreadMessages() : 0L;

            long pendingQuestions = 0L;
            long openQuestionReports = 0L;
            long failedGenerationJobs = 0L;
            if (learningAdminStatsService != null) {
                try {
                    pendingQuestions = learningAdminStatsService.countPendingQuestions();
                    openQuestionReports = learningAdminStatsService.countOpenQuestionReports();
                    failedGenerationJobs = learningAdminStatsService.countFailedGenerationJobsSince(OffsetDateTime.now().minusDays(7));
                } catch (Exception e) {
                    log.warn("Failed to load learning admin stats for dashboard", e);
                }
            }

            pendingActionsSection = new PendingActionsSectionDto(
                    commPending.ordersAwaitingFulfillment(),
                    commPending.paymentsNeedingReview(),
                    pendingRegs,
                    unreadMsgs,
                    pendingQuestions,
                    openQuestionReports,
                    failedGenerationJobs
            );
        } catch (Exception e) {
            log.error("Failed to load pending actions dashboard section", e);
            sectionErrors.put("pendingActions", "Failed to load pending actions");
        }

        // 6. Catalog
        CatalogSectionDto catalogSection = null;
        try {
            CatalogStatisticsDto catStats = catalogStatisticsService.getCatalogStatistics(lowStockThreshold);
            catalogSection = new CatalogSectionDto(
                    catStats.publishedNotes(),
                    catStats.publishedVideos(),
                    catStats.draftProducts(),
                    catStats.lowStockCount(),
                    catStats.lowStockItems()
            );
        } catch (Exception e) {
            log.error("Failed to load catalog dashboard section", e);
            sectionErrors.put("catalog", "Failed to load catalog metrics");
        }

        // 7. Recent Activities
        RecentActivitiesSectionDto recentSection = null;
        try {
            List<RecentOrderDto> orders = commerceStatisticsService.getRecentOrders(5);
            var classRegs = classStats != null && classStats.recentRegistrations() != null
                    ? classStats.recentRegistrations()
                    : Collections.<com.allearning.backend.classes.dto.RecentRegistrationDto>emptyList();
            var contactMsgs = commStats != null && commStats.recentMessages() != null
                    ? commStats.recentMessages()
                    : Collections.<com.allearning.backend.communication.dto.RecentContactMessageDto>emptyList();

            recentSection = new RecentActivitiesSectionDto(orders, classRegs, contactMsgs);
        } catch (Exception e) {
            log.error("Failed to load recent activities dashboard section", e);
            sectionErrors.put("recent", "Failed to load recent activities");
        }

        return new DashboardSummaryResponse(
                usersSection,
                salesSection,
                pendingActionsSection,
                catalogSection,
                classesSection,
                communicationSection,
                recentSection,
                sectionErrors,
                OffsetDateTime.now()
        );
    }

    @Override
    public RevenueSeriesDto getRevenue(Integer days) {
        int d = days != null ? days : 30;
        if (d != 7 && d != 30 && d != 90) {
            throw new IllegalArgumentException("days must be one of 7, 30, or 90");
        }
        return commerceStatisticsService.getDailyRevenue(d, configuredZone);
    }

    @Override
    public List<TopProductDto> getTopProducts(Integer days, Integer limit) {
        int d = days != null ? days : 30;
        if (d != 7 && d != 30 && d != 90) {
            throw new IllegalArgumentException("days must be one of 7, 30, or 90");
        }
        int l = limit != null ? limit : 5;
        if (l < 1 || l > 10) {
            throw new IllegalArgumentException("limit must be between 1 and 10");
        }
        return commerceStatisticsService.getTopProducts(d, l, configuredZone);
    }
}
