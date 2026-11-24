package com.myledger.service;

import com.myledger.domain.CompanionType;
import com.myledger.domain.Transaction;
import com.myledger.domain.TransactionType;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    public static Specification<Transaction> withFilters(
            LocalDate startDate,
            LocalDate endDate,
            TransactionType type,
            Long categoryId,
            CompanionType companionType,
            Boolean fixedOnly,
            String keyword
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("transactionDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("transactionDate"), endDate));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (companionType != null) {
                predicates.add(cb.equal(root.get("companionType"), companionType));
            }
            if (Boolean.TRUE.equals(fixedOnly)) {
                predicates.add(cb.isTrue(root.get("fixed")));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.like(cb.lower(cb.coalesce(root.get("memo"), "")), pattern));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
