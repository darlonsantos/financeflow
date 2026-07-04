package com.financeflow.categories.repository;

import com.financeflow.categories.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    
    @Query("SELECT c FROM Category c WHERE c.user.id = :userId AND c.deletedAt IS NULL")
    List<Category> findAllByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT c FROM Category c WHERE c.user.id = :userId AND c.type = :type AND c.deletedAt IS NULL")
    List<Category> findAllByUserIdAndType(@Param("userId") UUID userId, @Param("type") Category.CategoryType type);
    
    @Query("SELECT c FROM Category c WHERE c.id = :id AND c.user.id = :userId AND c.deletedAt IS NULL")
    Optional<Category> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);
    
    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE c.id = :id AND c.user.id = :userId AND c.deletedAt IS NULL")
    boolean existsByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);
    
    @Query("SELECT c FROM Category c WHERE c.parent.id = :parentId AND c.deletedAt IS NULL")
    List<Category> findAllByParentId(@Param("parentId") UUID parentId);
}
