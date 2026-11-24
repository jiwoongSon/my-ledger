package com.myledger.service;

import com.myledger.domain.AnalyticsPeriod;
import com.myledger.domain.CompanionType;
import com.myledger.domain.TransactionType;
import com.myledger.dto.DashboardAnalyticsDto;
import com.myledger.dto.DashboardAnalyticsDto.CategoryBreakdownItem;
import com.myledger.dto.DashboardAnalyticsDto.CompanionComparisonDto;
import com.myledger.dto.DashboardAnalyticsDto.FixedVariableSplitDto;
import com.myledger.dto.DashboardAnalyticsDto.MonthlyTrendItem;
import com.myledger.repository.TransactionRepository;
import com.myledger.repository.projection.CategoryTotalProjection;
import com.myledger.repository.projection.CompanionTotalProjection;
import com.myledger.repository.projection.FixedSplitProjection;
import com.myledger.repository.projection.MonthlyTrendProjection;
import com.myledger.repository.projection.TypeTotalProjection;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    public static final List<String> FOOD_AND_CAFE_CATEGORIES = List.of("Food", "Cafe & Leisure");

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final DateTimeFormatter DAILY_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter WEEK_START_FORMATTER = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter SHORT_MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMM yy", Locale.ENGLISH);

    private final TransactionRepository transactionRepository;
    private final Clock clock;

    public AnalyticsService(TransactionRepository transactionRepository, Clock clock) {
        this.transactionRepository = transactionRepository;
        this.clock = clock;
    }

    public DashboardAnalyticsDto getAnalytics(AnalyticsPeriod period, LocalDate requestedDate) {
        AnalyticsPeriod effectivePeriod = period != null ? period : AnalyticsPeriod.MONTHLY;
        LocalDate anchorDate = requestedDate != null ? requestedDate : LocalDate.now(clock);
        DateWindow window = resolveDateWindow(effectivePeriod, anchorDate);

        // 1. KPI Totals (DB GROUP BY type)
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        for (TypeTotalProjection row : transactionRepository.sumByTypeBetween(window.startDate(), window.endDate())) {
            BigDecimal amount = zeroIfNull(row.getTotalAmount());
            if (row.getType() == TransactionType.INCOME) {
                totalIncome = amount;
            } else if (row.getType() == TransactionType.EXPENSE) {
                totalExpense = amount;
            }
        }
        BigDecimal netBalance = totalIncome.subtract(totalExpense);
        BigDecimal savingsRate = computeSavingsRate(totalIncome, netBalance);

        // 2. Expense Breakdown by Category (DB GROUP BY category)
        List<CategoryBreakdownItem> categoryBreakdown = new ArrayList<>();
        for (CategoryTotalProjection row : transactionRepository.sumExpenseByCategoryBetween(window.startDate(), window.endDate())) {
            BigDecimal amount = zeroIfNull(row.getTotalAmount());
            categoryBreakdown.add(new CategoryBreakdownItem(
                    row.getCategoryName(),
                    amount,
                    computePercentage(amount, totalExpense),
                    row.getTransactionCount() != null ? row.getTransactionCount() : 0L
            ));
        }

        // 3. Food & Cafe Companion Pattern (Alone vs With Others) (DB GROUP BY companion_type)
        CompanionComparisonDto companionComparison = buildCompanionComparison(window.startDate(), window.endDate());

        // 4. Fixed vs Variable Expense Split (DB GROUP BY is_fixed)
        FixedVariableSplitDto fixedVariableSplit = buildFixedVariableSplit(window.startDate(), window.endDate(), totalExpense);

        // 5. Monthly Income vs Expense Trend (DB GROUP BY year, month, type)
        List<MonthlyTrendItem> monthlyTrend = buildMonthlyTrend(effectivePeriod, anchorDate);

        return new DashboardAnalyticsDto(
                effectivePeriod,
                anchorDate,
                window.startDate(),
                window.endDate(),
                window.label(),
                window.prevDate(),
                window.nextDate(),
                totalIncome,
                totalExpense,
                netBalance,
                savingsRate,
                categoryBreakdown,
                companionComparison,
                fixedVariableSplit,
                monthlyTrend
        );
    }

    private CompanionComparisonDto buildCompanionComparison(LocalDate startDate, LocalDate endDate) {
        BigDecimal aloneAmount = BigDecimal.ZERO;
        long aloneCount = 0L;
        BigDecimal withOthersAmount = BigDecimal.ZERO;
        long withOthersCount = 0L;
        BigDecimal unspecifiedAmount = BigDecimal.ZERO;
        long unspecifiedCount = 0L;

        List<CompanionTotalProjection> rows = transactionRepository.sumExpenseByCompanionForCategoriesBetween(
                startDate, endDate, FOOD_AND_CAFE_CATEGORIES
        );
        for (CompanionTotalProjection row : rows) {
            BigDecimal amount = zeroIfNull(row.getTotalAmount());
            long count = row.getTransactionCount() != null ? row.getTransactionCount() : 0L;
            if (row.getCompanionType() == CompanionType.ALONE) {
                aloneAmount = amount;
                aloneCount = count;
            } else if (row.getCompanionType() == CompanionType.WITH_OTHERS) {
                withOthersAmount = amount;
                withOthersCount = count;
            } else {
                unspecifiedAmount = unspecifiedAmount.add(amount);
                unspecifiedCount += count;
            }
        }

        BigDecimal totalFoodCafe = aloneAmount.add(withOthersAmount).add(unspecifiedAmount);
        BigDecimal comparedTotal = aloneAmount.add(withOthersAmount);

        return new CompanionComparisonDto(
                aloneAmount,
                aloneCount,
                computePercentage(aloneAmount, comparedTotal),
                withOthersAmount,
                withOthersCount,
                computePercentage(withOthersAmount, comparedTotal),
                unspecifiedAmount,
                unspecifiedCount,
                totalFoodCafe
        );
    }

    private FixedVariableSplitDto buildFixedVariableSplit(LocalDate startDate, LocalDate endDate, BigDecimal totalExpense) {
        BigDecimal fixedAmount = BigDecimal.ZERO;
        long fixedCount = 0L;
        BigDecimal variableAmount = BigDecimal.ZERO;
        long variableCount = 0L;

        for (FixedSplitProjection row : transactionRepository.sumExpenseByFixedBetween(startDate, endDate)) {
            BigDecimal amount = zeroIfNull(row.getTotalAmount());
            long count = row.getTransactionCount() != null ? row.getTransactionCount() : 0L;
            if (Boolean.TRUE.equals(row.getFixed())) {
                fixedAmount = amount;
                fixedCount = count;
            } else {
                variableAmount = amount;
                variableCount = count;
            }
        }

        return new FixedVariableSplitDto(
                fixedAmount,
                fixedCount,
                computePercentage(fixedAmount, totalExpense),
                variableAmount,
                variableCount,
                computePercentage(variableAmount, totalExpense),
                totalExpense
        );
    }

    private List<MonthlyTrendItem> buildMonthlyTrend(AnalyticsPeriod period, LocalDate anchorDate) {
        YearMonth startMonth;
        YearMonth endMonth;

        if (period == AnalyticsPeriod.YEARLY) {
            startMonth = YearMonth.of(anchorDate.getYear(), 1);
            endMonth = YearMonth.of(anchorDate.getYear(), 12);
        } else {
            endMonth = YearMonth.from(anchorDate);
            startMonth = endMonth.minusMonths(11);
        }

        List<MonthlyTrendProjection> rows = transactionRepository.sumMonthlyTrendBetween(
                startMonth.atDay(1),
                endMonth.atEndOfMonth()
        );

        Map<YearMonth, BigDecimal> incomeByMonth = new HashMap<>();
        Map<YearMonth, BigDecimal> expenseByMonth = new HashMap<>();

        for (MonthlyTrendProjection row : rows) {
            YearMonth ym = YearMonth.of(row.getYear(), row.getMonth());
            BigDecimal amount = zeroIfNull(row.getTotalAmount());
            if (row.getType() == TransactionType.INCOME) {
                incomeByMonth.put(ym, amount);
            } else if (row.getType() == TransactionType.EXPENSE) {
                expenseByMonth.put(ym, amount);
            }
        }

        List<MonthlyTrendItem> items = new ArrayList<>();
        YearMonth cursor = startMonth;
        while (!cursor.isAfter(endMonth)) {
            BigDecimal income = incomeByMonth.getOrDefault(cursor, BigDecimal.ZERO);
            BigDecimal expense = expenseByMonth.getOrDefault(cursor, BigDecimal.ZERO);
            items.add(new MonthlyTrendItem(
                    cursor.toString(),
                    cursor.format(SHORT_MONTH_FORMATTER),
                    income,
                    expense,
                    income.subtract(expense)
            ));
            cursor = cursor.plusMonths(1);
        }
        return items;
    }

    public DateWindow resolveDateWindow(AnalyticsPeriod period, LocalDate anchorDate) {
        return switch (period) {
            case DAILY -> new DateWindow(
                    anchorDate,
                    anchorDate,
                    anchorDate.format(DAILY_FORMATTER),
                    anchorDate.minusDays(1),
                    anchorDate.plusDays(1)
            );
            case WEEKLY -> {
                LocalDate monday = anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate sunday = monday.plusDays(6);
                String label = monday.format(WEEK_START_FORMATTER) + " – " + sunday.format(DAILY_FORMATTER);
                yield new DateWindow(monday, sunday, label, monday.minusWeeks(1), monday.plusWeeks(1));
            }
            case MONTHLY -> {
                YearMonth ym = YearMonth.from(anchorDate);
                LocalDate firstDay = ym.atDay(1);
                LocalDate lastDay = ym.atEndOfMonth();
                yield new DateWindow(firstDay, lastDay, ym.format(MONTH_FORMATTER), firstDay.minusMonths(1), firstDay.plusMonths(1));
            }
            case YEARLY -> {
                LocalDate firstDay = LocalDate.of(anchorDate.getYear(), 1, 1);
                LocalDate lastDay = LocalDate.of(anchorDate.getYear(), 12, 31);
                yield new DateWindow(firstDay, lastDay, String.valueOf(anchorDate.getYear()), firstDay.minusYears(1), firstDay.plusYears(1));
            }
        };
    }

    public static BigDecimal computeSavingsRate(BigDecimal totalIncome, BigDecimal netBalance) {
        if (totalIncome == null || totalIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return netBalance.multiply(ONE_HUNDRED).divide(totalIncome, 1, RoundingMode.HALF_UP);
    }

    public static BigDecimal computePercentage(BigDecimal part, BigDecimal total) {
        if (part == null || total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return part.multiply(ONE_HUNDRED).divide(total, 1, RoundingMode.HALF_UP);
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    public record DateWindow(
            LocalDate startDate,
            LocalDate endDate,
            String label,
            LocalDate prevDate,
            LocalDate nextDate
    ) {
    }
}
