package com.allearning.backend.learning.controller;

import com.allearning.backend.learning.dto.AdminChapterAnalyticsDetailResponse;
import com.allearning.backend.learning.dto.AdminLearningOverviewResponse;
import com.allearning.backend.learning.dto.AdminStudentLearningDetailResponse;
import com.allearning.backend.learning.service.AdminLearningAnalyticsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/learning-analytics")
@PreAuthorize("hasRole('ADMIN')")
public class AdminLearningAnalyticsController {

    private final AdminLearningAnalyticsService adminLearningAnalyticsService;

    public AdminLearningAnalyticsController(AdminLearningAnalyticsService adminLearningAnalyticsService) {
        this.adminLearningAnalyticsService = adminLearningAnalyticsService;
    }

    @GetMapping("/overview")
    public AdminLearningOverviewResponse overview(@RequestParam(required = false) Long subjectId) {
        return adminLearningAnalyticsService.getOverview(subjectId);
    }

    @GetMapping("/chapters/{chapterId}")
    public AdminChapterAnalyticsDetailResponse chapter(@PathVariable Long chapterId) {
        return adminLearningAnalyticsService.getChapterDetail(chapterId);
    }

    @GetMapping("/students/{userId}")
    public AdminStudentLearningDetailResponse student(@PathVariable Long userId) {
        return adminLearningAnalyticsService.getStudentDetail(userId);
    }
}
