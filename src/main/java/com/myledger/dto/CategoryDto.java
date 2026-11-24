package com.myledger.dto;

import com.myledger.domain.Category;
import com.myledger.domain.TransactionType;

public record CategoryDto(
        Long id,
        String name,
        TransactionType type,
        int displayOrder
) {
    public static CategoryDto from(Category category) {
        return new CategoryDto(
                category.getId(),
                category.getName(),
                category.getType(),
                category.getDisplayOrder()
        );
    }
}
