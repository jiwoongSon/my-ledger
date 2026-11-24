package com.myledger.dto;

import java.math.BigDecimal;

public record TransactionSummaryDto(
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netBalance,
        long transactionCount
) {
}
