package com.myledger.service;

import com.myledger.dto.RecurringSummaryDto.BatchExecutionResult;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RecurringTransactionScheduler {

    private final RecurringTransactionService recurringTransactionService;
    private final Clock clock;

    public RecurringTransactionScheduler(
            RecurringTransactionService recurringTransactionService,
            Clock clock
    ) {
        this.recurringTransactionService = recurringTransactionService;
        this.clock = clock;
    }

    /**
     * Runs daily at 00:05 KST (or configured app.timezone) to record active recurring transactions due today.
     */
    @Scheduled(cron = "0 5 0 * * *", zone = "${app.timezone:Asia/Seoul}")
    public BatchExecutionResult runDailyRecurringBatch() {
        return recurringTransactionService.processDueTransactions(LocalDate.now(clock));
    }
}
