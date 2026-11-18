package com.allearning.backend.admin.controller;

import com.allearning.backend.admin.dto.DashboardSummaryResponse;
import com.allearning.backend.admin.service.DashboardService;
import com.allearning.backend.commerce.dto.RevenueSeriesDto;
import com.allearning.backend.commerce.dto.TopProductDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    public AdminDashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }

    @GetMapping("/revenue")
    public ResponseEntity<RevenueSeriesDto> getRevenue(
            @RequestParam(name = "days", required = false) Integer days
    ) {
        return ResponseEntity.ok(dashboardService.getRevenue(days));
    }

    @GetMapping("/top-products")
    public ResponseEntity<List<TopProductDto>> getTopProducts(
            @RequestParam(name = "days", required = false) Integer days,
            @RequestParam(name = "limit", required = false) Integer limit
    ) {
        return ResponseEntity.ok(dashboardService.getTopProducts(days, limit));
    }
}
