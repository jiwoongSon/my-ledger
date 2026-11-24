package com.myledger.web;

import com.myledger.domain.AnalyticsPeriod;
import com.myledger.domain.TransactionType;
import com.myledger.dto.CategoryDto;
import com.myledger.dto.DashboardAnalyticsDto;
import com.myledger.service.AnalyticsService;
import com.myledger.service.CategoryService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AnalyticsApiController {

    private final AnalyticsService analyticsService;
    private final CategoryService categoryService;

    public AnalyticsApiController(AnalyticsService analyticsService, CategoryService categoryService) {
        this.analyticsService = analyticsService;
        this.categoryService = categoryService;
    }

    @GetMapping("/analytics")
    public DashboardAnalyticsDto getAnalytics(
            @RequestParam(defaultValue = "MONTHLY") AnalyticsPeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return analyticsService.getAnalytics(period, date);
    }

    @GetMapping("/categories")
    public List<CategoryDto> getCategories(@RequestParam(required = false) TransactionType type) {
        if (type == null) {
            return categoryService.findAllDtos();
        }
        return categoryService.findDtosByType(type);
    }
}
