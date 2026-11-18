package com.allearning.backend.admin.service;

import com.allearning.backend.admin.dto.DashboardSummaryResponse;
import com.allearning.backend.commerce.dto.RevenueSeriesDto;
import com.allearning.backend.commerce.dto.TopProductDto;

import java.util.List;

public interface DashboardService {

    DashboardSummaryResponse getSummary();

    RevenueSeriesDto getRevenue(Integer days);

    List<TopProductDto> getTopProducts(Integer days, Integer limit);
}
