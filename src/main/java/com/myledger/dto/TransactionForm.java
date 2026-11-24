package com.myledger.dto;

import com.myledger.domain.CompanionType;
import com.myledger.domain.Transaction;
import com.myledger.domain.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.format.annotation.DateTimeFormat;

public class TransactionForm {

    @NotNull(message = "Please select a transaction type.")
    private TransactionType type = TransactionType.EXPENSE;

    @NotNull(message = "Please select a category.")
    private Long categoryId;

    @NotNull(message = "Please enter an amount.")
    @DecimalMin(value = "1", message = "Amount must be greater than 0.")
    @Digits(integer = 12, fraction = 0, message = "Amount must be a whole KRW number up to 12 digits.")
    private BigDecimal amount;

    @NotNull(message = "Please select a date.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate transactionDate;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime transactionTime;

    private CompanionType companionType = CompanionType.NONE;

    private boolean fixed = false;

    @Size(max = 255, message = "Memo must be 255 characters or fewer.")
    private String memo;

    public static TransactionForm emptyForDate(LocalDate defaultDate) {
        TransactionForm form = new TransactionForm();
        form.setType(TransactionType.EXPENSE);
        form.setTransactionDate(defaultDate);
        form.setCompanionType(CompanionType.NONE);
        form.setFixed(false);
        return form;
    }

    public static TransactionForm fromEntity(Transaction transaction) {
        TransactionForm form = new TransactionForm();
        form.setType(transaction.getType());
        form.setCategoryId(transaction.getCategory().getId());
        form.setAmount(transaction.getAmount());
        form.setTransactionDate(transaction.getTransactionDate());
        form.setTransactionTime(transaction.getTransactionTime());
        form.setCompanionType(transaction.getCompanionType());
        form.setFixed(transaction.isFixed());
        form.setMemo(transaction.getMemo());
        return form;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public LocalTime getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(LocalTime transactionTime) {
        this.transactionTime = transactionTime;
    }

    public CompanionType getCompanionType() {
        return companionType;
    }

    public void setCompanionType(CompanionType companionType) {
        this.companionType = companionType != null ? companionType : CompanionType.NONE;
    }

    public boolean isFixed() {
        return fixed;
    }

    public void setFixed(boolean fixed) {
        this.fixed = fixed;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }
}
