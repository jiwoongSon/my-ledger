package com.myledger.dto;

import com.myledger.domain.AnalyticsPeriod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardAnalyticsDto(
        AnalyticsPeriod period,
        LocalDate anchorDate,
        LocalDate startDate,
        LocalDate endDate,
        String periodLabel,
        LocalDate prevDate,
        LocalDate nextDate,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netBalance,
        BigDecimal savingsRate,
        List<CategoryBreakdownItem> expenseByCategory,
        CompanionComparisonDto foodCafeCompanion,
        FixedVariableSplitDto fixedVsVariable,
        List<MonthlyTrendItem> monthlyTrend
) {

    public record CategoryBreakdownItem(
            String categoryName,
            BigDecimal amount,
            BigDecimal percentage,
            long count
    ) {
    }

    public record CompanionComparisonDto(
            BigDecimal aloneAmount,
            long aloneCount,
            BigDecimal alonePercentage,
            BigDecimal withOthersAmount,
            long withOthersCount,
            BigDecimal withOthersPercentage,
            BigDecimal unspecifiedAmount,
            long unspecifiedCount,
            BigDecimal totalAmount
    ) {
    }

    public record FixedVariableSplitDto(
            BigDecimal fixedAmount,
            long fixedCount,
            BigDecimal fixedPercentage,
            BigDecimal variableAmount,
            long variableCount,
            BigDecimal variablePercentage,
            BigDecimal totalExpense
    ) {
    }

    public record MonthlyTrendItem(
            String yearMonth,
            String label,
            BigDecimal income,
            BigDecimal expense,
            BigDecimal net
    ) {
    }
}
