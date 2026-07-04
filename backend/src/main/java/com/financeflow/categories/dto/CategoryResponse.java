package com.financeflow.categories.dto;

import com.financeflow.categories.domain.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
    
    private UUID id;
    private String name;
    private Category.CategoryType type;
    private String color;
    private String icon;
    private UUID parentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * @deprecated Use {@link com.financeflow.categories.mapper.CategoryMapper#toResponse(Category)} instead
     */
    @Deprecated
    public static CategoryResponse fromEntity(Category category) {
        return CategoryResponse.builder()
            .id(category.getId())
            .name(category.getName())
            .type(category.getType())
            .color(category.getColor())
            .icon(category.getIcon())
            .parentId(category.getParent() != null ? category.getParent().getId() : null)
            .createdAt(category.getCreatedAt())
            .updatedAt(category.getUpdatedAt())
            .build();
    }
}
