package com.myledger.service;

import com.myledger.domain.Category;
import com.myledger.domain.CompanionType;
import com.myledger.domain.RecurringTransaction;
import com.myledger.domain.Transaction;
import com.myledger.domain.TransactionType;
import com.myledger.dto.RecurringSummaryDto;
import com.myledger.dto.RecurringSummaryDto.BatchExecutionResult;
import com.myledger.dto.RecurringTransactionForm;
import com.myledger.repository.CategoryRepository;
import com.myledger.repository.RecurringTransactionRepository;
import com.myledger.repository.TransactionRepository;
import com.myledger.repository.projection.TypeTotalProjection;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RecurringTransactionService {

    private static final Logger log = LoggerFactory.getLogger(RecurringTransactionService.class);

    private final RecurringTransactionRepository recurringRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    public RecurringTransactionService(
            RecurringTransactionRepository recurringRepository,
            TransactionRepository transactionRepository,
            CategoryRepository categoryRepository
    ) {
        this.recurringRepository = recurringRepository;
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<RecurringTransaction> findAll() {
        return recurringRepository.findAllByOrderByActiveDescDayOfMonthAscIdAsc();
    }

    public RecurringTransaction getById(Long id) {
        return recurringRepository.findWithCategoryById(id)
                .orElseThrow(() -> new NoSuchElementException("Recurring transaction not found: " + id));
    }

    public RecurringSummaryDto getSummary() {
        long total = recurringRepository.count();
        long active = recurringRepository.countByActiveTrue();
        BigDecimal activeExpense = BigDecimal.ZERO;
        BigDecimal activeIncome = BigDecimal.ZERO;

        for (TypeTotalProjection row : recurringRepository.sumActiveByType()) {
            BigDecimal amount = row.getTotalAmount() != null ? row.getTotalAmount() : BigDecimal.ZERO;
            if (row.getType() == TransactionType.EXPENSE) {
                activeExpense = amount;
            } else if (row.getType() == TransactionType.INCOME) {
                activeIncome = amount;
            }
        }

        return new RecurringSummaryDto(total, active, activeExpense, activeIncome);
    }

    @Transactional
    public RecurringTransaction create(RecurringTransactionForm form) {
        Category category = resolveAndValidateCategory(form.getCategoryId(), form.getType());
        RecurringTransaction recurring = new RecurringTransaction(
                form.getType(),
                category,
                form.getAmount(),
                form.getDayOfMonth(),
                form.getMemo(),
                form.isActive()
        );
        return recurringRepository.save(recurring);
    }

    @Transactional
    public RecurringTransaction update(Long id, RecurringTransactionForm form) {
        RecurringTransaction existing = getById(id);
        Category category = resolveAndValidateCategory(form.getCategoryId(), form.getType());
        existing.update(
                form.getType(),
                category,
                form.getAmount(),
                form.getDayOfMonth(),
                form.getMemo(),
                form.isActive()
        );
        return recurringRepository.save(existing);
    }

    @Transactional
    public RecurringTransaction toggleActive(Long id) {
        RecurringTransaction existing = getById(id);
        existing.toggleActive();
        return recurringRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        RecurringTransaction existing = getById(id);
        // Keep historical transactions intact by detaching the recurring FK first.
        transactionRepository.detachRecurringTransaction(existing.getId());
        recurringRepository.delete(existing);
    }

    /**
     * Executes due recurring transactions for the target date idempotently.
     * Any recurring rule already recorded in the target month is skipped to prevent duplicates.
     */
    @Transactional
    public BatchExecutionResult processDueTransactions(LocalDate targetDate) {
        int dayOfMonth = targetDate.getDayOfMonth();
        YearMonth currentMonth = YearMonth.from(targetDate);
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();

        List<RecurringTransaction> dueItems = recurringRepository.findByActiveTrueAndDayOfMonthOrderByIdAsc(dayOfMonth);
        int created = 0;
        int skipped = 0;

        for (RecurringTransaction recurring : dueItems) {
            boolean alreadyRecordedThisMonth = transactionRepository.existsByRecurringTransactionIdAndTransactionDateBetween(
                    recurring.getId(),
                    monthStart,
                    monthEnd
            );
            if (alreadyRecordedThisMonth) {
                skipped++;
                continue;
            }

            Transaction generated = new Transaction(
                    recurring.getType(),
                    recurring.getCategory(),
                    recurring,
                    recurring.getAmount(),
                    targetDate,
                    null,
                    CompanionType.NONE,
                    true,
                    recurring.getMemo()
            );
            transactionRepository.save(generated);
            created++;
        }

        log.info(
                "Recurring batch finished for {} (day {}): due={}, created={}, skipped={}",
                targetDate, dayOfMonth, dueItems.size(), created, skipped
        );
        return new BatchExecutionResult(targetDate, dueItems.size(), created, skipped);
    }

    private Category resolveAndValidateCategory(Long categoryId, TransactionType expectedType) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Selected category does not exist."));
        if (category.getType() != expectedType) {
            throw new IllegalArgumentException(
                    "Category '" + category.getName() + "' does not match transaction type " + expectedType.getLabel() + "."
            );
        }
        return category;
    }
}
