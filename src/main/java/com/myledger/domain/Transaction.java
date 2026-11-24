package com.myledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(
        name = "transactions",
        indexes = {
                @Index(name = "idx_transactions_date_type", columnList = "transaction_date, type"),
                @Index(name = "idx_transactions_recurring_date", columnList = "recurring_id, transaction_date")
        }
)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 10, nullable = false)
    private TransactionType type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurring_id")
    private RecurringTransaction recurringTransaction;

    @Column(name = "amount", precision = 12, scale = 0, nullable = false)
    private BigDecimal amount;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "transaction_time")
    private LocalTime transactionTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "companion_type", length = 20, nullable = false)
    private CompanionType companionType = CompanionType.NONE;

    @Column(name = "is_fixed", nullable = false)
    private boolean fixed = false;

    @Column(name = "memo", length = 255)
    private String memo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Transaction() {
    }

    public Transaction(
            TransactionType type,
            Category category,
            BigDecimal amount,
            LocalDate transactionDate,
            LocalTime transactionTime,
            CompanionType companionType,
            boolean fixed,
            String memo
    ) {
        this(type, category, null, amount, transactionDate, transactionTime, companionType, fixed, memo);
    }

    public Transaction(
            TransactionType type,
            Category category,
            RecurringTransaction recurringTransaction,
            BigDecimal amount,
            LocalDate transactionDate,
            LocalTime transactionTime,
            CompanionType companionType,
            boolean fixed,
            String memo
    ) {
        this.type = type;
        this.category = category;
        this.recurringTransaction = recurringTransaction;
        this.amount = normalizeAmount(amount);
        this.transactionDate = transactionDate;
        this.transactionTime = transactionTime;
        this.companionType = companionType != null ? companionType : CompanionType.NONE;
        this.fixed = fixed;
        this.memo = normalizeMemo(memo);
    }

    public void update(
            TransactionType type,
            Category category,
            BigDecimal amount,
            LocalDate transactionDate,
            LocalTime transactionTime,
            CompanionType companionType,
            boolean fixed,
            String memo
    ) {
        this.type = type;
        this.category = category;
        this.amount = normalizeAmount(amount);
        this.transactionDate = transactionDate;
        this.transactionTime = transactionTime;
        this.companionType = companionType != null ? companionType : CompanionType.NONE;
        this.fixed = fixed;
        this.memo = normalizeMemo(memo);
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.companionType == null) {
            this.companionType = CompanionType.NONE;
        }
        this.amount = normalizeAmount(this.amount);
        this.memo = normalizeMemo(this.memo);
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.companionType == null) {
            this.companionType = CompanionType.NONE;
        }
        this.amount = normalizeAmount(this.amount);
        this.memo = normalizeMemo(this.memo);
    }

    private static BigDecimal normalizeAmount(BigDecimal value) {
        return value == null ? null : value.setScale(0, RoundingMode.UNNECESSARY);
    }

    private static String normalizeMemo(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public Long getId() {
        return id;
    }

    public TransactionType getType() {
        return type;
    }

    public Category getCategory() {
        return category;
    }

    public RecurringTransaction getRecurringTransaction() {
        return recurringTransaction;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public LocalTime getTransactionTime() {
        return transactionTime;
    }

    public CompanionType getCompanionType() {
        return companionType;
    }

    public boolean isFixed() {
        return fixed;
    }

    public String getMemo() {
        return memo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
