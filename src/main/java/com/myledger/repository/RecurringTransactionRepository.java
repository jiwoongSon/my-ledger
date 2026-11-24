package com.myledger.repository;

import com.myledger.domain.RecurringTransaction;
import com.myledger.repository.projection.TypeTotalProjection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {

    @EntityGraph(attributePaths = "category")
    List<RecurringTransaction> findAllByOrderByActiveDescDayOfMonthAscIdAsc();

    @EntityGraph(attributePaths = "category")
    Optional<RecurringTransaction> findWithCategoryById(Long id);

    @EntityGraph(attributePaths = "category")
    List<RecurringTransaction> findByActiveTrueAndDayOfMonthOrderByIdAsc(int dayOfMonth);

    long countByActiveTrue();

    @Query("""
            SELECT r.type AS type, COALESCE(SUM(r.amount), 0) AS totalAmount
            FROM RecurringTransaction r
            WHERE r.active = true
            GROUP BY r.type
            """)
    List<TypeTotalProjection> sumActiveByType();
}
