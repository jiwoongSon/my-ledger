package com.myledger.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myledger.domain.TransactionType;
import com.myledger.dto.RecurringTransactionForm;
import com.myledger.dto.TransactionForm;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MoneyAndValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDownValidator() {
        validatorFactory.close();
    }

    @Test
    @DisplayName("Calculates BigDecimal net balance and savings rate with exact HALF_UP rounding")
    void calculatesSavingsRateAndPercentagesWithBigDecimalPrecision() {
        // Income: 3,333,333 KRW, Expense: 1,111,111 KRW -> Net: 2,222,222 KRW -> 66.7%
        BigDecimal income = new BigDecimal("3333333");
        BigDecimal expense = new BigDecimal("1111111");
        BigDecimal net = income.subtract(expense);

        BigDecimal savingsRate = AnalyticsService.computeSavingsRate(income, net);
        assertThat(net).isEqualByComparingTo("2222222");
        assertThat(savingsRate).isEqualByComparingTo("66.7");

        // Negative savings rate when spending exceeds income
        BigDecimal overspentNet = new BigDecimal("-500000");
        assertThat(AnalyticsService.computeSavingsRate(new BigDecimal("2000000"), overspentNet))
                .isEqualByComparingTo("-25.0");

        // Zero income produces null savings rate (no division by zero)
        assertThat(AnalyticsService.computeSavingsRate(BigDecimal.ZERO, new BigDecimal("-10000"))).isNull();

        // Large 12-digit KRW arithmetic preserves full precision without double drift
        BigDecimal largeIncome = new BigDecimal("999999999999");
        BigDecimal largeExpense = new BigDecimal("123456789012");
        assertThat(largeIncome.subtract(largeExpense)).isEqualByComparingTo("876543210987");

        // Percentage calculation handles zero denominator safely
        assertThat(AnalyticsService.computePercentage(new BigDecimal("15000"), BigDecimal.ZERO))
                .isEqualByComparingTo("0.0");
    }

    @Test
    @DisplayName("Rejects zero, negative, and fractional amounts on TransactionForm")
    void rejectsInvalidAmountsOnTransactionForm() {
        TransactionForm zeroForm = validTransactionForm(BigDecimal.ZERO);
        assertThat(validator.validate(zeroForm))
                .extracting(ConstraintViolation::getPropertyPath)
                .anyMatch(p -> p.toString().equals("amount"));

        TransactionForm negativeForm = validTransactionForm(new BigDecimal("-15000"));
        assertThat(validator.validate(negativeForm))
                .extracting(ConstraintViolation::getPropertyPath)
                .anyMatch(p -> p.toString().equals("amount"));

        TransactionForm fractionalForm = validTransactionForm(new BigDecimal("12500.50"));
        assertThat(validator.validate(fractionalForm))
                .extracting(ConstraintViolation::getPropertyPath)
                .anyMatch(p -> p.toString().equals("amount"));

        TransactionForm validForm = validTransactionForm(new BigDecimal("12500"));
        assertThat(validator.validate(validForm)).isEmpty();
    }

    @Test
    @DisplayName("Validates RecurringTransactionForm amount and dayOfMonth (1..28)")
    void validatesRecurringTransactionFormConstraints() {
        RecurringTransactionForm invalidDayLow = validRecurringForm(new BigDecimal("15000"), 0);
        assertThat(validator.validate(invalidDayLow))
                .extracting(ConstraintViolation::getPropertyPath)
                .anyMatch(p -> p.toString().equals("dayOfMonth"));

        RecurringTransactionForm invalidDayHigh = validRecurringForm(new BigDecimal("15000"), 29);
        assertThat(validator.validate(invalidDayHigh))
                .extracting(ConstraintViolation::getPropertyPath)
                .anyMatch(p -> p.toString().equals("dayOfMonth"));

        RecurringTransactionForm invalidAmount = validRecurringForm(new BigDecimal("-100"), 15);
        assertThat(validator.validate(invalidAmount))
                .extracting(ConstraintViolation::getPropertyPath)
                .anyMatch(p -> p.toString().equals("amount"));

        Set<ConstraintViolation<RecurringTransactionForm>> validViolations =
                validator.validate(validRecurringForm(new BigDecimal("15000"), 28));
        assertThat(validViolations).isEmpty();
    }

    private static TransactionForm validTransactionForm(BigDecimal amount) {
        TransactionForm form = TransactionForm.emptyForDate(LocalDate.of(2026, 11, 24));
        form.setType(TransactionType.EXPENSE);
        form.setCategoryId(1L);
        form.setAmount(amount);
        return form;
    }

    private static RecurringTransactionForm validRecurringForm(BigDecimal amount, int dayOfMonth) {
        RecurringTransactionForm form = new RecurringTransactionForm();
        form.setType(TransactionType.EXPENSE);
        form.setCategoryId(1L);
        form.setAmount(amount);
        form.setDayOfMonth(dayOfMonth);
        form.setActive(true);
        return form;
    }
}
