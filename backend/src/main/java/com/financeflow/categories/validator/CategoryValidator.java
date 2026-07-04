package com.financeflow.categories.validator;

import com.financeflow.categories.domain.Category;
import com.financeflow.categories.repository.CategoryRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Validador de regras de negócio para categorias.
 */
@Component
public class CategoryValidator {
    
    private final CategoryRepository categoryRepository;
    
    public CategoryValidator(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }
    
    /**
     * Valida se uma categoria pode ser excluída.
     * Uma categoria não pode ser excluída se tiver subcategorias.
     * 
     * @param categoryId ID da categoria a ser excluída
     * @throws IllegalStateException se a categoria tem subcategorias
     */
    public void validateCanDelete(UUID categoryId) {
        List<Category> subcategories = categoryRepository.findAllByParentId(categoryId);
        if (!subcategories.isEmpty()) {
            throw new IllegalStateException(
                String.format("Não é possível excluir categoria com %d subcategoria(s)", 
                    subcategories.size())
            );
        }
    }
    
    /**
     * Valida se uma categoria pai existe e pertence ao usuário.
     * 
     * @param parentId ID da categoria pai
     * @param userId ID do usuário
     * @return Categoria pai se válida
     * @throws IllegalArgumentException se a categoria pai não for encontrada
     */
    public Category validateParentCategory(UUID parentId, UUID userId) {
        if (parentId == null) {
            return null;
        }
        
        return categoryRepository.findByIdAndUserId(parentId, userId)
            .orElseThrow(() -> new IllegalArgumentException(
                String.format("Categoria pai não encontrada: %s", parentId)
            ));
    }
    
    /**
     * Valida se não há referência circular ao definir uma categoria pai.
     * 
     * @param categoryId ID da categoria
     * @param parentId ID da categoria pai proposta
     * @throws IllegalArgumentException se houver referência circular
     */
    public void validateNoCircularReference(UUID categoryId, UUID parentId) {
        if (categoryId.equals(parentId)) {
            throw new IllegalArgumentException(
                "Uma categoria não pode ser pai de si mesma"
            );
        }
        
        // Verificar se a categoria pai não tem a categoria atual como ancestral
        Category parent = categoryRepository.findById(parentId).orElse(null);
        if (parent != null) {
            Category current = parent;
            while (current.getParent() != null) {
                if (current.getParent().getId().equals(categoryId)) {
                    throw new IllegalArgumentException(
                        "Referência circular detectada: a categoria pai é descendente desta categoria"
                    );
                }
                current = current.getParent();
            }
        }
    }
}
