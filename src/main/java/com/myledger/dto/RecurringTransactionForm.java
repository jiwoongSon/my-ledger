package com.myledger.dto;

import com.myledger.domain.RecurringTransaction;
import com.myledger.domain.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class RecurringTransactionForm {

    @NotNull(message = "Please select a transaction type.")
    private TransactionType type = TransactionType.EXPENSE;

    @NotNull(message = "Please select a category.")
    private Long categoryId;

    @NotNull(message = "Please enter an amount.")
    @DecimalMin(value = "1", message = "Amount must be greater than 0.")
    @Digits(integer = 12, fraction = 0, message = "Amount must be a whole KRW number up to 12 digits.")
    private BigDecimal amount;

    @NotNull(message = "Please select a day of month (1 to 28).")
    @Min(value = 1, message = "Day of month must be between 1 and 28.")
    @Max(value = 28, message = "Day of month must be between 1 and 28.")
    private Integer dayOfMonth = 1;

    @Size(max = 255, message = "Memo must be 255 characters or fewer.")
    private String memo;

    private boolean active = true;

    public static RecurringTransactionForm defaultForm(int defaultDay) {
        RecurringTransactionForm form = new RecurringTransactionForm();
        form.setType(TransactionType.EXPENSE);
        form.setDayOfMonth(Math.min(Math.max(defaultDay, 1), 28));
        form.setActive(true);
        return form;
    }

    public static RecurringTransactionForm fromEntity(RecurringTransaction entity) {
        RecurringTransactionForm form = new RecurringTransactionForm();
        form.setType(entity.getType());
        form.setCategoryId(entity.getCategory().getId());
        form.setAmount(entity.getAmount());
        form.setDayOfMonth(entity.getDayOfMonth());
        form.setMemo(entity.getMemo());
        form.setActive(entity.isActive());
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

    public Integer getDayOfMonth() {
        return dayOfMonth;
    }

    public void setDayOfMonth(Integer dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
