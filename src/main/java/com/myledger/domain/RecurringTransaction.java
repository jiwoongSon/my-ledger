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
import java.time.LocalDateTime;

@Entity
@Table(
        name = "recurring_transactions",
        indexes = {
                @Index(name = "idx_recurring_active_day", columnList = "is_active, day_of_month")
        }
)
public class RecurringTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 10, nullable = false)
    private TransactionType type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "amount", precision = 12, scale = 0, nullable = false)
    private BigDecimal amount;

    @Column(name = "day_of_month", nullable = false)
    private int dayOfMonth;

    @Column(name = "memo", length = 255)
    private String memo;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected RecurringTransaction() {
    }

    public RecurringTransaction(
            TransactionType type,
            Category category,
            BigDecimal amount,
            int dayOfMonth,
            String memo,
            boolean active
    ) {
        this.type = type;
        this.category = category;
        this.amount = normalizeAmount(amount);
        this.dayOfMonth = dayOfMonth;
        this.memo = normalizeMemo(memo);
        this.active = active;
    }

    public void update(
            TransactionType type,
            Category category,
            BigDecimal amount,
            int dayOfMonth,
            String memo,
            boolean active
    ) {
        this.type = type;
        this.category = category;
        this.amount = normalizeAmount(amount);
        this.dayOfMonth = dayOfMonth;
        this.memo = normalizeMemo(memo);
        this.active = active;
    }

    public void toggleActive() {
        this.active = !this.active;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.amount = normalizeAmount(this.amount);
        this.memo = normalizeMemo(this.memo);
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
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

    public BigDecimal getAmount() {
        return amount;
    }

    public int getDayOfMonth() {
        return dayOfMonth;
    }

    public String getMemo() {
        return memo;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
