package com.myledger.web;

import com.myledger.domain.AnalyticsPeriod;
import com.myledger.domain.CompanionType;
import com.myledger.domain.TransactionType;
import com.myledger.dto.DashboardAnalyticsDto;
import com.myledger.dto.TransactionForm;
import com.myledger.service.AnalyticsService;
import com.myledger.service.CategoryService;
import com.myledger.service.TransactionService;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DashboardController {

    private final AnalyticsService analyticsService;
    private final TransactionService transactionService;
    private final CategoryService categoryService;
    private final Clock clock;

    public DashboardController(
            AnalyticsService analyticsService,
            TransactionService transactionService,
            CategoryService categoryService,
            Clock clock
    ) {
        this.analyticsService = analyticsService;
        this.transactionService = transactionService;
        this.categoryService = categoryService;
        this.clock = clock;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(
            @RequestParam(defaultValue = "MONTHLY") AnalyticsPeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model
    ) {
        LocalDate today = LocalDate.now(clock);
        LocalDate anchorDate = date != null ? date : today;
        DashboardAnalyticsDto analytics = analyticsService.getAnalytics(period, anchorDate);

        model.addAttribute("activePage", "dashboard");
        model.addAttribute("today", today);
        model.addAttribute("periods", AnalyticsPeriod.values());
        model.addAttribute("analytics", analytics);
        model.addAttribute("recentTransactions", transactionService.findRecent());
        model.addAttribute("categories", categoryService.findAllDtos());
        model.addAttribute("transactionTypes", TransactionType.values());
        model.addAttribute("companionTypes", CompanionType.values());

        if (!model.containsAttribute("transactionForm")) {
            model.addAttribute("transactionForm", TransactionForm.emptyForDate(today));
        }

        return "dashboard";
    }
}
