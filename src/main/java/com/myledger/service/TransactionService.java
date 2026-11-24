package com.myledger.service;

import com.myledger.domain.Category;
import com.myledger.domain.CompanionType;
import com.myledger.domain.Transaction;
import com.myledger.domain.TransactionType;
import com.myledger.dto.TransactionForm;
import com.myledger.dto.TransactionSummaryDto;
import com.myledger.repository.CategoryRepository;
import com.myledger.repository.TransactionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TransactionService {

    private static final Sort DEFAULT_LIST_SORT = Sort.by(
            Sort.Order.desc("transactionDate"),
            Sort.Order.desc("transactionTime"),
            Sort.Order.desc("id")
    );

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final EntityManager entityManager;

    public TransactionService(
            TransactionRepository transactionRepository,
            CategoryRepository categoryRepository,
            EntityManager entityManager
    ) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public Transaction create(TransactionForm form) {
        Category category = resolveAndValidateCategory(form.getCategoryId(), form.getType());
        CompanionType companionType = form.getType() == TransactionType.EXPENSE
                ? (form.getCompanionType() != null ? form.getCompanionType() : CompanionType.NONE)
                : CompanionType.NONE;
        boolean fixed = form.getType() == TransactionType.EXPENSE && form.isFixed();

        Transaction transaction = new Transaction(
                form.getType(),
                category,
                form.getAmount(),
                form.getTransactionDate(),
                form.getTransactionTime(),
                companionType,
                fixed,
                form.getMemo()
        );
        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction update(Long id, TransactionForm form) {
        Transaction existing = getById(id);
        Category category = resolveAndValidateCategory(form.getCategoryId(), form.getType());
        CompanionType companionType = form.getType() == TransactionType.EXPENSE
                ? (form.getCompanionType() != null ? form.getCompanionType() : CompanionType.NONE)
                : CompanionType.NONE;
        boolean fixed = form.getType() == TransactionType.EXPENSE && form.isFixed();

        existing.update(
                form.getType(),
                category,
                form.getAmount(),
                form.getTransactionDate(),
                form.getTransactionTime(),
                companionType,
                fixed,
                form.getMemo()
        );
        return transactionRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        Transaction existing = getById(id);
        transactionRepository.delete(existing);
    }

    public Transaction getById(Long id) {
        return transactionRepository.findWithCategoryById(id)
                .orElseThrow(() -> new NoSuchElementException("Transaction not found: " + id));
    }

    public List<Transaction> findRecent() {
        return transactionRepository.findTop8ByOrderByTransactionDateDescTransactionTimeDescIdDesc();
    }

    public List<Transaction> findFiltered(
            LocalDate startDate,
            LocalDate endDate,
            TransactionType type,
            Long categoryId,
            CompanionType companionType,
            Boolean fixedOnly,
            String keyword
    ) {
        Specification<Transaction> spec = TransactionSpecifications.withFilters(
                startDate, endDate, type, categoryId, companionType, fixedOnly, keyword
        );
        return transactionRepository.findAll(spec, DEFAULT_LIST_SORT);
    }

    /**
     * Computes filtered income, expense, net balance, and count directly in the database via GROUP BY.
     */
    public TransactionSummaryDto summarizeFiltered(
            LocalDate startDate,
            LocalDate endDate,
            TransactionType type,
            Long categoryId,
            CompanionType companionType,
            Boolean fixedOnly,
            String keyword
    ) {
        Specification<Transaction> spec = TransactionSpecifications.withFilters(
                startDate, endDate, type, categoryId, companionType, fixedOnly, keyword
        );

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<Transaction> root = cq.from(Transaction.class);
        Predicate predicate = spec.toPredicate(root, cq, cb);

        cq.multiselect(
                root.get("type").alias("type"),
                cb.coalesce(cb.sum(root.get("amount")), BigDecimal.ZERO).alias("totalAmount"),
                cb.count(root.get("id")).alias("txCount")
        );
        if (predicate != null) {
            cq.where(predicate);
        }
        cq.groupBy(root.get("type"));

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        long totalCount = 0L;

        for (Tuple row : entityManager.createQuery(cq).getResultList()) {
            TransactionType rowType = row.get("type", TransactionType.class);
            BigDecimal amount = row.get("totalAmount", BigDecimal.class);
            Long count = row.get("txCount", Long.class);
            if (rowType == TransactionType.INCOME) {
                totalIncome = amount != null ? amount : BigDecimal.ZERO;
            } else if (rowType == TransactionType.EXPENSE) {
                totalExpense = amount != null ? amount : BigDecimal.ZERO;
            }
            totalCount += (count != null ? count : 0L);
        }

        return new TransactionSummaryDto(
                totalIncome,
                totalExpense,
                totalIncome.subtract(totalExpense),
                totalCount
        );
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
