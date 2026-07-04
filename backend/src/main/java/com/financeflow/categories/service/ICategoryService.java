package com.financeflow.categories.service;

import com.financeflow.categories.domain.Category;
import com.financeflow.categories.dto.CategoryRequest;
import com.financeflow.categories.dto.CategoryResponse;

import java.util.List;
import java.util.UUID;

public interface ICategoryService {
    
    List<CategoryResponse> findAll();
    
    List<CategoryResponse> findByType(Category.CategoryType type);
    
    CategoryResponse findById(UUID id);
    
    CategoryResponse create(CategoryRequest request);
    
    CategoryResponse update(UUID id, CategoryRequest request);
    
    void delete(UUID id);
}
