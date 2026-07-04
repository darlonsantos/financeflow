package com.financeflow.categories.service;

import com.financeflow.categories.domain.Category;
import com.financeflow.categories.dto.CategoryRequest;
import com.financeflow.categories.dto.CategoryResponse;
import com.financeflow.categories.exception.CategoryNotFoundException;
import com.financeflow.categories.mapper.CategoryMapper;
import com.financeflow.categories.repository.CategoryRepository;
import com.financeflow.categories.validator.CategoryValidator;
import com.financeflow.audit.service.AuditService;
import com.financeflow.users.domain.User;
import com.financeflow.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService implements ICategoryService {
    
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CategoryMapper categoryMapper;
    private final CategoryValidator categoryValidator;
    private final AuditService auditService;
    
    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UUID)) {
            throw new RuntimeException("Usuário não autenticado");
        }
        return (UUID) authentication.getPrincipal();
    }
    
    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        UUID userId = getCurrentUserId();
        return categoryMapper.toResponseList(categoryRepository.findAllByUserId(userId));
    }
    
    @Transactional(readOnly = true)
    public List<CategoryResponse> findByType(Category.CategoryType type) {
        UUID userId = getCurrentUserId();
        return categoryMapper.toResponseList(categoryRepository.findAllByUserIdAndType(userId, type));
    }
    
    @Transactional(readOnly = true)
    public CategoryResponse findById(UUID id) {
        UUID userId = getCurrentUserId();
        Category category = categoryRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new CategoryNotFoundException(id));
        return categoryMapper.toResponse(category);
    }
    
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        UUID userId = getCurrentUserId();
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        // Validar categoria pai se fornecida
        Category parent = categoryValidator.validateParentCategory(request.getParentId(), userId);
        
        Category.CategoryBuilder builder = Category.builder()
            .user(user)
            .name(request.getName())
            .type(request.getType())
            .color(request.getColor())
            .icon(request.getIcon());
        
        if (parent != null) {
            builder.parent(parent);
        }
        
        Category category = builder.build();
        category = categoryRepository.save(category);
        
        // Auditoria
        auditService.logAction(
            userId,
            "CREATE",
            "Category",
            category.getId(),
            Map.of(
                "name", category.getName(),
                "type", category.getType().name(),
                "parentId", category.getParent() != null ? category.getParent().getId().toString() : "null"
            )
        );
        
        return categoryMapper.toResponse(category);
    }
    
    @Transactional
    public CategoryResponse update(UUID id, CategoryRequest request) {
        UUID userId = getCurrentUserId();
        Category category = categoryRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new CategoryNotFoundException(id));
        
        // Validar referência circular se parentId mudou
        if (request.getParentId() != null && !request.getParentId().equals(
            category.getParent() != null ? category.getParent().getId() : null)) {
            categoryValidator.validateNoCircularReference(id, request.getParentId());
        }
        
        category.setName(request.getName());
        category.setType(request.getType());
        category.setColor(request.getColor());
        category.setIcon(request.getIcon());
        
        // Atualizar parent usando validator
        Category parent = categoryValidator.validateParentCategory(request.getParentId(), userId);
        category.setParent(parent);
        
        category = categoryRepository.save(category);
        
        // Auditoria
        auditService.logAction(
            userId,
            "UPDATE",
            "Category",
            id,
            Map.of(
                "name", category.getName(),
                "type", category.getType().name(),
                "parentId", category.getParent() != null ? category.getParent().getId().toString() : "null"
            )
        );
        
        return categoryMapper.toResponse(category);
    }
    
    @Transactional
    public void delete(UUID id) {
        UUID userId = getCurrentUserId();
        Category category = categoryRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new CategoryNotFoundException(id));
        
        // Validar se pode excluir usando validator
        categoryValidator.validateCanDelete(id);
        
        category.setDeletedAt(java.time.LocalDateTime.now());
        categoryRepository.save(category);
        
        // Auditoria
        auditService.logAction(
            userId,
            "DELETE",
            "Category",
            id,
            Map.of(
                "name", category.getName(),
                "type", category.getType().name()
            )
        );
    }
}
