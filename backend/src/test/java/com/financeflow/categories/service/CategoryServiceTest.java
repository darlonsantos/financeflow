package com.financeflow.categories.service;

import com.financeflow.categories.domain.Category;
import com.financeflow.categories.dto.CategoryRequest;
import com.financeflow.categories.dto.CategoryResponse;
import com.financeflow.categories.exception.CategoryNotFoundException;
import com.financeflow.categories.repository.CategoryRepository;
import com.financeflow.users.domain.User;
import com.financeflow.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CategoryService categoryService;

    private UUID testUserId;
    private User testUser;
    private Category testCategory;
    private CategoryRequest categoryRequest;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = User.builder()
                .id(testUserId)
                .name("Test User")
                .email("test@example.com")
                .build();

        testCategory = Category.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .name("Test Category")
                .type(Category.CategoryType.EXPENSE)
                .color("#EF4444")
                .build();

        categoryRequest = new CategoryRequest();
        categoryRequest.setName("New Category");
        categoryRequest.setType(Category.CategoryType.INCOME);
        categoryRequest.setColor("#10B981");

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(testUserId);
    }

    @Test
    void testFindAll_Success() {
        // Arrange
        List<Category> categories = Arrays.asList(testCategory);
        when(categoryRepository.findAllByUserId(testUserId)).thenReturn(categories);

        // Act
        List<CategoryResponse> result = categoryService.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Category", result.get(0).getName());
        verify(categoryRepository).findAllByUserId(testUserId);
    }

    @Test
    void testFindByType_Success() {
        // Arrange
        List<Category> categories = Arrays.asList(testCategory);
        when(categoryRepository.findAllByUserIdAndType(testUserId, Category.CategoryType.EXPENSE))
                .thenReturn(categories);

        // Act
        List<CategoryResponse> result = categoryService.findByType(Category.CategoryType.EXPENSE);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(categoryRepository).findAllByUserIdAndType(testUserId, Category.CategoryType.EXPENSE);
    }

    @Test
    void testFindById_Success() {
        // Arrange
        UUID categoryId = testCategory.getId();
        when(categoryRepository.findByIdAndUserId(categoryId, testUserId))
                .thenReturn(Optional.of(testCategory));

        // Act
        CategoryResponse result = categoryService.findById(categoryId);

        // Assert
        assertNotNull(result);
        assertEquals("Test Category", result.getName());
        verify(categoryRepository).findByIdAndUserId(categoryId, testUserId);
    }

    @Test
    void testFindById_NotFound() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        when(categoryRepository.findByIdAndUserId(categoryId, testUserId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CategoryNotFoundException.class, () -> {
            categoryService.findById(categoryId);
        });
    }

    @Test
    void testCreate_Success() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

        // Act
        CategoryResponse result = categoryService.create(categoryRequest);

        // Assert
        assertNotNull(result);
        verify(userRepository).findById(testUserId);
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void testCreate_WithParent() {
        // Arrange
        UUID parentId = UUID.randomUUID();
        Category parentCategory = Category.builder()
                .id(parentId)
                .user(testUser)
                .name("Parent Category")
                .type(Category.CategoryType.EXPENSE)
                .build();

        categoryRequest.setParentId(parentId);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(categoryRepository.findByIdAndUserId(parentId, testUserId))
                .thenReturn(Optional.of(parentCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

        // Act
        CategoryResponse result = categoryService.create(categoryRequest);

        // Assert
        assertNotNull(result);
        verify(categoryRepository).findByIdAndUserId(parentId, testUserId);
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void testUpdate_Success() {
        // Arrange
        UUID categoryId = testCategory.getId();
        when(categoryRepository.findByIdAndUserId(categoryId, testUserId))
                .thenReturn(Optional.of(testCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

        // Act
        CategoryResponse result = categoryService.update(categoryId, categoryRequest);

        // Assert
        assertNotNull(result);
        verify(categoryRepository).findByIdAndUserId(categoryId, testUserId);
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void testDelete_Success() {
        // Arrange
        UUID categoryId = testCategory.getId();
        when(categoryRepository.findByIdAndUserId(categoryId, testUserId))
                .thenReturn(Optional.of(testCategory));
        when(categoryRepository.findAllByParentId(categoryId))
                .thenReturn(Collections.emptyList());
        when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

        // Act
        categoryService.delete(categoryId);

        // Assert
        verify(categoryRepository).findByIdAndUserId(categoryId, testUserId);
        verify(categoryRepository).findAllByParentId(categoryId);
        verify(categoryRepository).save(any(Category.class));
        assertNotNull(testCategory.getDeletedAt());
    }

    @Test
    void testDelete_WithSubcategories() {
        // Arrange
        UUID categoryId = testCategory.getId();
        Category subcategory = Category.builder()
                .id(UUID.randomUUID())
                .parent(testCategory)
                .build();

        when(categoryRepository.findByIdAndUserId(categoryId, testUserId))
                .thenReturn(Optional.of(testCategory));
        when(categoryRepository.findAllByParentId(categoryId))
                .thenReturn(Arrays.asList(subcategory));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            categoryService.delete(categoryId);
        });

        assertEquals("Não é possível excluir categoria com subcategorias", exception.getMessage());
        verify(categoryRepository).findAllByParentId(categoryId);
        verify(categoryRepository, never()).save(any(Category.class));
    }
}
