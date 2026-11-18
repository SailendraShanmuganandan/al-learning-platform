package com.allearning.backend.commerce.service;

import com.allearning.backend.commerce.dto.*;

import java.time.ZoneId;
import java.util.List;

/**
 * Service interface for aggregate commerce domain statistics.
 */
public interface CommerceStatisticsService {

    SalesStatisticsDto getSalesStatistics(ZoneId zone);

    CommercePendingActionsDto getPendingActions();

    List<RecentOrderDto> getRecentOrders(int limit);

    RevenueSeriesDto getDailyRevenue(int days, ZoneId zone);

    List<TopProductDto> getTopProducts(int days, int limit, ZoneId zone);
}
