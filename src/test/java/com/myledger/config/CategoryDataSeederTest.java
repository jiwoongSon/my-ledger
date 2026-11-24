package com.myledger.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.myledger.domain.Category;
import com.myledger.domain.TransactionType;
import com.myledger.repository.CategoryRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CategoryDataSeederTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CategoryDataSeeder categoryDataSeeder;

    @Test
    void seedsAllDefaultEnglishCategoriesIdempotently() {
        List<String> incomeNames = categoryRepository.findByTypeOrderByDisplayOrderAscNameAsc(TransactionType.INCOME)
                .stream()
                .map(Category::getName)
                .toList();
        List<String> expenseNames = categoryRepository.findByTypeOrderByDisplayOrderAscNameAsc(TransactionType.EXPENSE)
                .stream()
                .map(Category::getName)
                .toList();

        assertThat(incomeNames).containsExactly(
                "Allowance",
                "Part-time",
                "Salary",
                "Investment",
                "Other Income"
        );
        assertThat(expenseNames).containsExactly(
                "Subscription",
                "Transportation",
                "Food",
                "Cafe & Leisure",
                "Fashion & Beauty",
                "Health & Fitness",
                "Loan Interest",
                "Living Supplies",
                "Self-Development",
                "Other Expense"
        );

        long countBefore = categoryRepository.count();
        categoryDataSeeder.run(new DefaultApplicationArguments());
        assertThat(categoryRepository.count()).isEqualTo(countBefore);
    }
}
