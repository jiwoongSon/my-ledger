package com.myledger.repository;

import com.myledger.domain.Category;
import com.myledger.domain.TransactionType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByName(String name);

    Optional<Category> findByName(String name);

    List<Category> findAllByOrderByTypeAscDisplayOrderAscNameAsc();

    List<Category> findByTypeOrderByDisplayOrderAscNameAsc(TransactionType type);
}
