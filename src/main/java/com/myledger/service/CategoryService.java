package com.myledger.service;

import com.myledger.domain.Category;
import com.myledger.domain.TransactionType;
import com.myledger.dto.CategoryDto;
import com.myledger.repository.CategoryRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> findAll() {
        return categoryRepository.findAllByOrderByTypeAscDisplayOrderAscNameAsc();
    }

    public List<Category> findByType(TransactionType type) {
        return categoryRepository.findByTypeOrderByDisplayOrderAscNameAsc(type);
    }

    public List<CategoryDto> findAllDtos() {
        return findAll().stream().map(CategoryDto::from).toList();
    }

    public List<CategoryDto> findDtosByType(TransactionType type) {
        return findByType(type).stream().map(CategoryDto::from).toList();
    }
}
