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
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AnalyticsServiceTest {

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category salary;
    private Category food;
    private Category cafe;
    private Category subscription;
    private Category transportation;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        salary = categoryRepository.findByName("Salary").orElseThrow();
        food = categoryRepository.findByName("Food").orElseThrow();
        cafe = categoryRepository.findByName("Cafe & Leisure").orElseThrow();
        subscription = categoryRepository.findByName("Subscription").orElseThrow();
        transportation = categoryRepository.findByName("Transportation").orElseThrow();
    }

    @Test
    void aggregatesMonthlyKpisBreakdownsAndTrendsInDatabase() {
        // November 2026 transactions
        save(TransactionType.INCOME, salary, "3000000", LocalDate.of(2026, 11, 1), CompanionType.NONE, false, "Monthly salary");
        save(TransactionType.EXPENSE, food, "12000", LocalDate.of(2026, 11, 10), CompanionType.ALONE, false, "Solo lunch");
        save(TransactionType.EXPENSE, cafe, "18000", LocalDate.of(2026, 11, 12), CompanionType.WITH_OTHERS, false, "Coffee with friends");
        save(TransactionType.EXPENSE, food, "30000", LocalDate.of(2026, 11, 15), CompanionType.WITH_OTHERS, false, "Team dinner");
        save(TransactionType.EXPENSE, subscription, "15000", LocalDate.of(2026, 11, 5), CompanionType.NONE, true, "Streaming plan");
        save(TransactionType.EXPENSE, transportation, "65000", LocalDate.of(2026, 11, 3), CompanionType.ALONE, true, "Transit pass");

        // Prior month transaction for monthly trend
        save(TransactionType.EXPENSE, food, "20000", LocalDate.of(2026, 10, 20), CompanionType.ALONE, false, "October dinner");

        DashboardAnalyticsDto dto = analyticsService.getAnalytics(AnalyticsPeriod.MONTHLY, LocalDate.of(2026, 11, 24));

        assertThat(dto.startDate()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(dto.endDate()).isEqualTo(LocalDate.of(2026, 11, 30));

        // Total Expense = 12,000 + 18,000 + 30,000 + 15,000 + 65,000 = 140,000
        assertThat(dto.totalIncome()).isEqualByComparingTo("3000000");
        assertThat(dto.totalExpense()).isEqualByComparingTo("140000");
        assertThat(dto.netBalance()).isEqualByComparingTo("2860000");
        // Savings rate = (2,860,000 / 3,000,000) * 100 = 95.3%
        assertThat(dto.savingsRate()).isEqualByComparingTo("95.3");

        // Food & Cafe companion split (must exclude Transportation even though it was ALONE)
        assertThat(dto.foodCafeCompanion().aloneAmount()).isEqualByComparingTo("12000");
        assertThat(dto.foodCafeCompanion().aloneCount()).isEqualTo(1L);
        assertThat(dto.foodCafeCompanion().withOthersAmount()).isEqualByComparingTo("48000");
        assertThat(dto.foodCafeCompanion().withOthersCount()).isEqualTo(2L);
        assertThat(dto.foodCafeCompanion().alonePercentage()).isEqualByComparingTo("20.0");
        assertThat(dto.foodCafeCompanion().withOthersPercentage()).isEqualByComparingTo("80.0");

        // Fixed vs Variable split: Fixed = 15,000 + 65,000 = 80,000; Variable = 60,000
        assertThat(dto.fixedVsVariable().fixedAmount()).isEqualByComparingTo("80000");
        assertThat(dto.fixedVsVariable().fixedCount()).isEqualTo(2L);
        assertThat(dto.fixedVsVariable().variableAmount()).isEqualByComparingTo("60000");
        assertThat(dto.fixedVsVariable().variableCount()).isEqualTo(3L);

        // Monthly trend contains 12 entries ending at 2026-11
        assertThat(dto.monthlyTrend()).hasSize(12);
        assertThat(dto.monthlyTrend().get(10).yearMonth()).isEqualTo("2026-10");
        assertThat(dto.monthlyTrend().get(10).expense()).isEqualByComparingTo("20000");
        assertThat(dto.monthlyTrend().get(11).yearMonth()).isEqualTo("2026-11");
        assertThat(dto.monthlyTrend().get(11).income()).isEqualByComparingTo("3000000");
        assertThat(dto.monthlyTrend().get(11).expense()).isEqualByComparingTo("140000");
    }

    @Test
    void resolvesWeeklyMondayToSundayAndLeapYearMonthlyWindowsAccurately() {
        // 2026-11-24 is Tuesday -> Week is Mon 2026-11-23 to Sun 2026-11-29
        AnalyticsService.DateWindow weekWindow =
                analyticsService.resolveDateWindow(AnalyticsPeriod.WEEKLY, LocalDate.of(2026, 11, 24));
        assertThat(weekWindow.startDate()).isEqualTo(LocalDate.of(2026, 11, 23));
        assertThat(weekWindow.endDate()).isEqualTo(LocalDate.of(2026, 11, 29));

        // Leap year February 2024 ends on Feb 29
        AnalyticsService.DateWindow leapMonth =
                analyticsService.resolveDateWindow(AnalyticsPeriod.MONTHLY, LocalDate.of(2024, 2, 15));
        assertThat(leapMonth.startDate()).isEqualTo(LocalDate.of(2024, 2, 1));
        assertThat(leapMonth.endDate()).isEqualTo(LocalDate.of(2024, 2, 29));
    }

    @Test
    void returnsNullSavingsRateWhenNoIncomeExists() {
        save(TransactionType.EXPENSE, food, "10000", LocalDate.of(2026, 11, 24), CompanionType.ALONE, false, "Lunch");

        DashboardAnalyticsDto dto = analyticsService.getAnalytics(AnalyticsPeriod.DAILY, LocalDate.of(2026, 11, 24));
        assertThat(dto.totalIncome()).isEqualByComparingTo("0");
        assertThat(dto.totalExpense()).isEqualByComparingTo("10000");
        assertThat(dto.netBalance()).isEqualByComparingTo("-10000");
        assertThat(dto.savingsRate()).isNull();
    }

    private void save(
            TransactionType type,
            Category category,
            String amount,
            LocalDate date,
            CompanionType companion,
            boolean fixed,
            String memo
    ) {
        transactionRepository.save(new Transaction(
                type, category, new BigDecimal(amount), date, LocalTime.of(12, 0), companion, fixed, memo
        ));
    }
}
