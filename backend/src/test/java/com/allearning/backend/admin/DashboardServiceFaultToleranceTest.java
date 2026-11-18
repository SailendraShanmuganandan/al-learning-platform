package com.allearning.backend.admin;

import com.allearning.backend.admin.dto.DashboardSummaryResponse;
import com.allearning.backend.admin.service.DashboardServiceImpl;
import com.allearning.backend.auth.service.UserStatisticsService;
import com.allearning.backend.catalog.dto.CatalogStatisticsDto;
import com.allearning.backend.catalog.service.CatalogStatisticsService;
import com.allearning.backend.classes.dto.ClassStatisticsDto;
import com.allearning.backend.classes.service.ClassStatisticsService;
import com.allearning.backend.commerce.dto.CommercePendingActionsDto;
import com.allearning.backend.commerce.dto.SalesStatisticsDto;
import com.allearning.backend.commerce.service.CommerceStatisticsService;
import com.allearning.backend.communication.dto.CommunicationStatisticsDto;
import com.allearning.backend.communication.service.CommunicationStatisticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceFaultToleranceTest {

    @Mock
    private UserStatisticsService userStatisticsService;

    @Mock
    private CatalogStatisticsService catalogStatisticsService;

    @Mock
    private ClassStatisticsService classStatisticsService;

    @Mock
    private CommunicationStatisticsService communicationStatisticsService;

    @Mock
    private CommerceStatisticsService commerceStatisticsService;

    @Test
    @DisplayName("Resilience: Simulating a failure in userStatisticsService returns null users section and sectionErrors marker, but other sections return intact")
    void sectionFailureResilienceTest() {
        DashboardServiceImpl dashboardService = new DashboardServiceImpl(
                userStatisticsService,
                catalogStatisticsService,
                classStatisticsService,
                communicationStatisticsService,
                commerceStatisticsService,
                "Asia/Colombo",
                5
        );

        // Simulate failure in users section
        when(userStatisticsService.getUserStatistics(any())).thenThrow(new RuntimeException("Database timeout on user metrics"));

        // Other services succeed
        when(commerceStatisticsService.getSalesStatistics(any())).thenReturn(
                new SalesStatisticsDto(BigDecimal.valueOf(100), BigDecimal.valueOf(500), BigDecimal.valueOf(2000), BigDecimal.valueOf(5000), 10, 2)
        );
        when(commerceStatisticsService.getPendingActions()).thenReturn(
                new CommercePendingActionsDto(3, 1)
        );
        when(commerceStatisticsService.getRecentOrders(anyInt())).thenReturn(Collections.emptyList());

        when(catalogStatisticsService.getCatalogStatistics(anyInt())).thenReturn(
                new CatalogStatisticsDto(10, 5, 2, 1, Collections.emptyList())
        );

        when(classStatisticsService.getClassStatistics()).thenReturn(
                new ClassStatisticsDto(4, 2, 20, Collections.emptyList())
        );

        when(communicationStatisticsService.getCommunicationStatistics()).thenReturn(
                new CommunicationStatisticsDto(3, 2, 1, Collections.emptyList())
        );

        DashboardSummaryResponse response = dashboardService.getSummary();

        assertNotNull(response);
        // Failed section is null
        assertNull(response.users());
        // Error marker is present
        assertTrue(response.sectionErrors().containsKey("users"));
        assertEquals("Failed to load user metrics", response.sectionErrors().get("users"));

        // Other sections are intact
        assertNotNull(response.sales());
        assertEquals(BigDecimal.valueOf(100), response.sales().revenueToday());
        assertNotNull(response.catalog());
        assertEquals(10, response.catalog().publishedNotes());
        assertNotNull(response.classes());
        assertEquals(4, response.classes().activeClasses());
        assertNotNull(response.communication());
        assertEquals(2, response.communication().publishedAnnouncements());
        assertNotNull(response.pendingActions());
        assertEquals(3, response.pendingActions().ordersAwaitingFulfillment());
    }
}
