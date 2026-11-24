package com.myledger.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myledger.domain.Category;
import com.myledger.domain.RecurringTransaction;
import com.myledger.domain.Transaction;
import com.myledger.domain.TransactionType;
import com.myledger.dto.RecurringSummaryDto.BatchExecutionResult;
import com.myledger.repository.CategoryRepository;
import com.myledger.repository.RecurringTransactionRepository;
import com.myledger.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecurringTransactionSchedulerTest {

    @Autowired
    private RecurringTransactionService recurringService;

    @Autowired
    private RecurringTransactionRepository recurringRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private MockMvc mockMvc;

    private Category subscription;
    private Category loanInterest;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        recurringRepository.deleteAll();
        subscription = categoryRepository.findByName("Subscription").orElseThrow();
        loanInterest = categoryRepository.findByName("Loan Interest").orElseThrow();
    }

    @Test
    @DisplayName("Prevents duplicate transactions when batch runs twice on the same day")
    void preventsDuplicateExecutionWithinSameMonth() {
        RecurringTransaction netflix = recurringRepository.save(new RecurringTransaction(
                TransactionType.EXPENSE, subscription, new BigDecimal("17000"), 15, "Netflix Standard", true
        ));
        // Paused rule on same day must be ignored
        recurringRepository.save(new RecurringTransaction(
                TransactionType.EXPENSE, loanInterest, new BigDecimal("85000"), 15, "Paused loan", false
        ));
        // Different day rule must be ignored
        recurringRepository.save(new RecurringTransaction(
                TransactionType.EXPENSE, subscription, new BigDecimal("9900"), 20, "Cloud storage", true
        ));

        LocalDate runDate = LocalDate.of(2026, 11, 15);

        // First execution: inserts 1 transaction
        BatchExecutionResult firstRun = recurringService.processDueTransactions(runDate);
        assertThat(firstRun.dueCount()).isEqualTo(1);
        assertThat(firstRun.createdCount()).isEqualTo(1);
        assertThat(firstRun.skippedCount()).isEqualTo(0);

        // Second consecutive execution on the same day: skips duplicate
        BatchExecutionResult secondRun = recurringService.processDueTransactions(runDate);
        assertThat(secondRun.dueCount()).isEqualTo(1);
        assertThat(secondRun.createdCount()).isEqualTo(0);
        assertThat(secondRun.skippedCount()).isEqualTo(1);

        List<Transaction> saved = transactionRepository.findAll();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).isFixed()).isTrue();
        assertThat(saved.get(0).getAmount()).isEqualByComparingTo("17000");
        assertThat(saved.get(0).getTransactionDate()).isEqualTo(runDate);
        assertThat(saved.get(0).getRecurringTransaction().getId()).isEqualTo(netflix.getId());

        // Next month on the same day: inserts a fresh transaction for December
        BatchExecutionResult nextMonthRun = recurringService.processDueTransactions(LocalDate.of(2026, 12, 15));
        assertThat(nextMonthRun.createdCount()).isEqualTo(1);
        assertThat(transactionRepository.count()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Preserves historical transactions when a recurring rule is deleted")
    void preservesHistoricalTransactionsWhenRuleDeleted() {
        RecurringTransaction rule = recurringRepository.save(new RecurringTransaction(
                TransactionType.EXPENSE, subscription, new BigDecimal("14900"), 10, "Music app", true
        ));

        recurringService.processDueTransactions(LocalDate.of(2026, 11, 10));
        assertThat(transactionRepository.count()).isEqualTo(1L);

        recurringService.delete(rule.getId());

        assertThat(recurringRepository.count()).isZero();
        List<Transaction> remaining = transactionRepository.findAll();
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).getRecurringTransaction()).isNull();
        assertThat(remaining.get(0).getAmount()).isEqualByComparingTo("14900");
    }

    @Test
    @DisplayName("Supports Recurring page CRUD, Active/Paused toggle, and manual Run Now via MockMvc")
    void supportsRecurringWebFlow() throws Exception {
        mockMvc.perform(get("/recurring").with(user("testadmin").roles("OWNER")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/recurring")
                        .with(user("testadmin").roles("OWNER"))
                        .with(csrf())
                        .param("type", "EXPENSE")
                        .param("categoryId", subscription.getId().toString())
                        .param("amount", "12000")
                        .param("dayOfMonth", "24")
                        .param("memo", "Gym membership")
                        .param("active", "true"))
                .andExpect(status().is3xxRedirection());

        RecurringTransaction saved = recurringRepository.findAll().get(0);
        assertThat(saved.isActive()).isTrue();

        // Toggle active -> paused
        mockMvc.perform(post("/recurring/" + saved.getId() + "/toggle")
                        .with(user("testadmin").roles("OWNER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(recurringRepository.findById(saved.getId()).orElseThrow().isActive()).isFalse();

        // Manual Run Now endpoint works cleanly
        mockMvc.perform(post("/recurring/run-now")
                        .with(user("testadmin").roles("OWNER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
    }
}
