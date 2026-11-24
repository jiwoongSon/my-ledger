package com.myledger.config;

import com.myledger.domain.Category;
import com.myledger.domain.TransactionType;
import com.myledger.repository.CategoryRepository;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CategoryDataSeeder implements ApplicationRunner {

    private record SeedCategory(String name, TransactionType type, int displayOrder) {
    }

    private static final List<SeedCategory> DEFAULT_CATEGORIES = List.of(
            // INCOME categories
            new SeedCategory("Allowance", TransactionType.INCOME, 1),
            new SeedCategory("Part-time", TransactionType.INCOME, 2),
            new SeedCategory("Salary", TransactionType.INCOME, 3),
            new SeedCategory("Investment", TransactionType.INCOME, 4),
            new SeedCategory("Other Income", TransactionType.INCOME, 5),

            // EXPENSE categories
            new SeedCategory("Subscription", TransactionType.EXPENSE, 1),
            new SeedCategory("Transportation", TransactionType.EXPENSE, 2),
            new SeedCategory("Food", TransactionType.EXPENSE, 3),
            new SeedCategory("Cafe & Leisure", TransactionType.EXPENSE, 4),
            new SeedCategory("Fashion & Beauty", TransactionType.EXPENSE, 5),
            new SeedCategory("Health & Fitness", TransactionType.EXPENSE, 6),
            new SeedCategory("Loan Interest", TransactionType.EXPENSE, 7),
            new SeedCategory("Living Supplies", TransactionType.EXPENSE, 8),
            new SeedCategory("Self-Development", TransactionType.EXPENSE, 9),
            new SeedCategory("Other Expense", TransactionType.EXPENSE, 10)
    );

    private final CategoryRepository categoryRepository;

    public CategoryDataSeeder(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (SeedCategory seed : DEFAULT_CATEGORIES) {
            if (!categoryRepository.existsByName(seed.name())) {
                categoryRepository.save(new Category(seed.name(), seed.type(), seed.displayOrder()));
            }
        }
    }
}
