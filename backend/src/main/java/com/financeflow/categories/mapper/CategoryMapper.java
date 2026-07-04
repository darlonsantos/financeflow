package com.financeflow.categories.mapper;

import com.financeflow.categories.domain.Category;
import com.financeflow.categories.dto.CategoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CategoryMapper {
    
    @Mapping(target = "parentId", source = "parent.id")
    CategoryResponse toResponse(Category category);
    
    List<CategoryResponse> toResponseList(List<Category> categories);
}
