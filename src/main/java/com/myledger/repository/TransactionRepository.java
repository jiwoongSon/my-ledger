package com.myledger.repository;

import com.myledger.domain.Transaction;
import com.myledger.repository.projection.CategoryTotalProjection;
import com.myledger.repository.projection.CompanionTotalProjection;
import com.myledger.repository.projection.FixedSplitProjection;
import com.myledger.repository.projection.MonthlyTrendProjection;
import com.myledger.repository.projection.TypeTotalProjection;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    @Override
    @EntityGraph(attributePaths = "category")
    List<Transaction> findAll(Specification<Transaction> spec, Sort sort);

    @EntityGraph(attributePaths = "category")
    Optional<Transaction> findWithCategoryById(Long id);

    @EntityGraph(attributePaths = "category")
    List<Transaction> findTop8ByOrderByTransactionDateDescTransactionTimeDescIdDesc();

    @Query("""
            SELECT t.type AS type, COALESCE(SUM(t.amount), 0) AS totalAmount
            FROM Transaction t
            WHERE t.transactionDate BETWEEN :startDate AND :endDate
            GROUP BY t.type
            """)
    List<TypeTotalProjection> sumByTypeBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            SELECT c.id AS categoryId,
                   c.name AS categoryName,
                   COALESCE(SUM(t.amount), 0) AS totalAmount,
                   COUNT(t.id) AS transactionCount
            FROM Transaction t
            JOIN t.category c
            WHERE t.type = com.myledger.domain.TransactionType.EXPENSE
              AND t.transactionDate BETWEEN :startDate AND :endDate
            GROUP BY c.id, c.name
            ORDER BY SUM(t.amount) DESC, c.name ASC
            """)
    List<CategoryTotalProjection> sumExpenseByCategoryBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            SELECT t.companionType AS companionType,
                   COALESCE(SUM(t.amount), 0) AS totalAmount,
                   COUNT(t.id) AS transactionCount
            FROM Transaction t
            JOIN t.category c
            WHERE t.type = com.myledger.domain.TransactionType.EXPENSE
              AND c.name IN :categoryNames
              AND t.transactionDate BETWEEN :startDate AND :endDate
            GROUP BY t.companionType
            """)
    List<CompanionTotalProjection> sumExpenseByCompanionForCategoriesBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("categoryNames") Collection<String> categoryNames
    );

    @Query("""
            SELECT t.fixed AS fixed,
                   COALESCE(SUM(t.amount), 0) AS totalAmount,
                   COUNT(t.id) AS transactionCount
            FROM Transaction t
            WHERE t.type = com.myledger.domain.TransactionType.EXPENSE
              AND t.transactionDate BETWEEN :startDate AND :endDate
            GROUP BY t.fixed
            """)
    List<FixedSplitProjection> sumExpenseByFixedBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            SELECT YEAR(t.transactionDate) AS year,
                   MONTH(t.transactionDate) AS month,
                   t.type AS type,
                   COALESCE(SUM(t.amount), 0) AS totalAmount
            FROM Transaction t
            WHERE t.transactionDate BETWEEN :startDate AND :endDate
            GROUP BY YEAR(t.transactionDate), MONTH(t.transactionDate), t.type
            ORDER BY YEAR(t.transactionDate) ASC, MONTH(t.transactionDate) ASC
            """)
    List<MonthlyTrendProjection> sumMonthlyTrendBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
