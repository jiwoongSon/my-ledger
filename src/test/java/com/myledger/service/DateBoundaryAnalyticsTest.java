package com.myledger.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myledger.domain.AnalyticsPeriod;
import com.myledger.domain.Category;
import com.myledger.domain.CompanionType;
import com.myledger.domain.Transaction;
import com.myledger.domain.TransactionType;
import com.myledger.dto.DashboardAnalyticsDto;
import com.myledger.repository.CategoryRepository;
import com.myledger.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class DateBoundaryAnalyticsTest {

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category food;
    private Category salary;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        food = categoryRepository.findByName("Food").orElseThrow();
        salary = categoryRepository.findByName("Salary").orElseThrow();
    }

    @Test
    @DisplayName("Includes Feb 29 in leap year monthly aggregation and excludes Jan 31 and Mar 1")
    void aggregatesLeapYearFebruaryBoundariesAccurately() {
        saveExpense(LocalDate.of(2024, 1, 31), "10000");  // excluded
        saveExpense(LocalDate.of(2024, 2, 1), "20000");   // included
        saveExpense(LocalDate.of(2024, 2, 29), "35000");  // included (leap day)
        saveExpense(LocalDate.of(2024, 3, 1), "40000");   // excluded

        DashboardAnalyticsDto dto = analyticsService.getAnalytics(AnalyticsPeriod.MONTHLY, LocalDate.of(2024, 2, 29));

        assertThat(dto.startDate()).isEqualTo(LocalDate.of(2024, 2, 1));
        assertThat(dto.endDate()).isEqualTo(LocalDate.of(2024, 2, 29));
        assertThat(dto.totalExpense()).isEqualByComparingTo("55000");
    }

    @Test
    @DisplayName("Handles 30-day (April) and 31-day (March) month-end boundaries accurately")
    void aggregates30And31DayMonthEndBoundaries() {
        saveExpense(LocalDate.of(2026, 2, 28), "5000");
        saveExpense(LocalDate.of(2026, 3, 1), "11000");
        saveExpense(LocalDate.of(2026, 3, 31), "22000");
        saveExpense(LocalDate.of(2026, 4, 1), "33000");
        saveExpense(LocalDate.of(2026, 4, 30), "44000");
        saveExpense(LocalDate.of(2026, 5, 1), "55000");

        DashboardAnalyticsDto march = analyticsService.getAnalytics(AnalyticsPeriod.MONTHLY, LocalDate.of(2026, 3, 15));
        assertThat(march.startDate()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(march.endDate()).isEqualTo(LocalDate.of(2026, 3, 31));
        assertThat(march.totalExpense()).isEqualByComparingTo("33000");

        DashboardAnalyticsDto april = analyticsService.getAnalytics(AnalyticsPeriod.MONTHLY, LocalDate.of(2026, 4, 30));
        assertThat(april.startDate()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(april.endDate()).isEqualTo(LocalDate.of(2026, 4, 30));
        assertThat(april.totalExpense()).isEqualByComparingTo("77000");
    }

    @Test
    @DisplayName("Includes Monday through Sunday strictly for weekly aggregation including year crossover")
    void aggregatesWeeklyMondayToSundayAcrossYearBoundary() {
        // Anchor: 2026-01-01 (Thursday) -> Week is Monday 2025-12-29 to Sunday 2026-01-04
        saveExpense(LocalDate.of(2025, 12, 28), "9000");  // prior Sunday -> excluded
        saveExpense(LocalDate.of(2025, 12, 29), "15000"); // Monday -> included
        saveIncome(LocalDate.of(2026, 1, 1), "100000");   // Thursday -> included
        saveExpense(LocalDate.of(2026, 1, 4), "25000");   // Sunday -> included
        saveExpense(LocalDate.of(2026, 1, 5), "8000");    // next Monday -> excluded

        DashboardAnalyticsDto weekly = analyticsService.getAnalytics(AnalyticsPeriod.WEEKLY, LocalDate.of(2026, 1, 1));

        assertThat(weekly.startDate()).isEqualTo(LocalDate.of(2025, 12, 29));
        assertThat(weekly.endDate()).isEqualTo(LocalDate.of(2026, 1, 4));
        assertThat(weekly.totalIncome()).isEqualByComparingTo("100000");
        assertThat(weekly.totalExpense()).isEqualByComparingTo("40000");
        assertThat(weekly.netBalance()).isEqualByComparingTo("60000");
        assertThat(weekly.savingsRate()).isEqualByComparingTo("60.0");
    }

    private void saveExpense(LocalDate date, String amount) {
        transactionRepository.save(new Transaction(
                TransactionType.EXPENSE, food, new BigDecimal(amount), date, null, CompanionType.ALONE, false, null
        ));
    }

    private void saveIncome(LocalDate date, String amount) {
        transactionRepository.save(new Transaction(
                TransactionType.INCOME, salary, new BigDecimal(amount), date, null, CompanionType.NONE, false, null
        ));
    }
}
