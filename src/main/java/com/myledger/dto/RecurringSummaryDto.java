package com.myledger.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringSummaryDto(
        long totalCount,
        long activeCount,
        BigDecimal activeMonthlyExpense,
        BigDecimal activeMonthlyIncome
) {
    public record BatchExecutionResult(
            LocalDate executionDate,
            int dueCount,
            int createdCount,
            int skippedCount
    ) {
    }
}
