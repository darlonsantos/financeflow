package com.financeflow.budgets.repository;

import com.financeflow.budgets.domain.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    @Query("SELECT b FROM Budget b JOIN FETCH b.category WHERE b.user.id = :userId AND b.deletedAt IS NULL " +
           "AND b.month BETWEEN :startMonth AND :endMonth")
    List<Budget> findAllByUserIdAndMonthRange(
        @Param("userId") UUID userId,
        @Param("startMonth") LocalDate startMonth,
        @Param("endMonth") LocalDate endMonth
    );

    @Query("SELECT b FROM Budget b JOIN FETCH b.category WHERE b.user.id = :userId AND b.deletedAt IS NULL " +
           "ORDER BY b.month DESC, b.createdAt DESC")
    List<Budget> findAllByUserId(@Param("userId") UUID userId);

    @Query("SELECT b FROM Budget b JOIN FETCH b.category WHERE b.id = :id AND b.user.id = :userId AND b.deletedAt IS NULL")
    Optional<Budget> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    boolean existsByUser_IdAndCategory_IdAndMonthAndDeletedAtIsNull(UUID userId, UUID categoryId, LocalDate month);

    boolean existsByUser_IdAndCategory_IdAndMonthAndDeletedAtIsNullAndIdNot(
        UUID userId,
        UUID categoryId,
        LocalDate month,
        UUID id
    );
}
