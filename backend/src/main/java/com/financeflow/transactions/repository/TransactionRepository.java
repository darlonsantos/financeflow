package com.financeflow.transactions.repository;

import com.financeflow.transactions.domain.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.deletedAt IS NULL")
    Page<Transaction> findAllByUserId(@Param("userId") UUID userId, Pageable pageable);
    
    @Query("SELECT t FROM Transaction t JOIN FETCH t.account JOIN FETCH t.category JOIN FETCH t.user WHERE t.id = :id AND t.user.id = :userId AND t.deletedAt IS NULL")
    Optional<Transaction> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);
    
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.account.id = :accountId AND t.deletedAt IS NULL")
    Page<Transaction> findAllByUserIdAndAccountId(@Param("userId") UUID userId, @Param("accountId") UUID accountId, Pageable pageable);
    
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.category.id = :categoryId AND t.deletedAt IS NULL")
    Page<Transaction> findAllByUserIdAndCategoryId(@Param("userId") UUID userId, @Param("categoryId") UUID categoryId, Pageable pageable);
    
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.type = :type AND t.deletedAt IS NULL")
    Page<Transaction> findAllByUserIdAndType(@Param("userId") UUID userId, @Param("type") Transaction.TransactionType type, Pageable pageable);
    
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.recurring = true AND t.deletedAt IS NULL")
    List<Transaction> findAllRecurringByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.user.id = :userId " +
           "AND t.account.id = :accountId AND t.category.id = :categoryId " +
           "AND t.amount = :amount AND t.type = :type " +
           "AND t.date BETWEEN :startDate AND :endDate " +
           "AND t.deletedAt IS NULL " +
           "AND (t.description = :description OR (t.description IS NULL AND :description IS NULL))")
    long countMatchingTransactions(
        @Param("userId") UUID userId,
        @Param("accountId") UUID accountId,
        @Param("categoryId") UUID categoryId,
        @Param("amount") java.math.BigDecimal amount,
        @Param("type") Transaction.TransactionType type,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("description") String description
    );
    
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.date BETWEEN :startDate AND :endDate AND t.deletedAt IS NULL")
    Page<Transaction> findAllByUserIdAndDateBetween(@Param("userId") UUID userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);
    
    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId AND t.deletedAt IS NULL")
    List<Transaction> findAllByAccountId(@Param("accountId") UUID accountId);

    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId AND t.deletedAt IS NULL")
    Page<Transaction> findAllByAccountId(@Param("accountId") UUID accountId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.account.id IN :accountIds AND t.deletedAt IS NULL")
    Page<Transaction> findAllByAccountIdIn(@Param("accountIds") List<UUID> accountIds, Pageable pageable);
    
    @Query("SELECT t FROM Transaction t WHERE t.clientId = :clientId AND t.deletedAt IS NULL")
    Optional<Transaction> findByClientId(@Param("clientId") String clientId);

    @Query("SELECT t FROM Transaction t JOIN FETCH t.account JOIN FETCH t.category WHERE t.clientId = :clientId AND t.user.id = :userId AND t.deletedAt IS NULL")
    Optional<Transaction> findByClientIdAndUserId(@Param("clientId") String clientId, @Param("userId") UUID userId);
    
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId " +
           "AND t.deletedAt IS NULL " +
           "ORDER BY t.date DESC")
    List<Transaction> findAllByUserIdForReport(@Param("userId") UUID userId);
    
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.account.id = :accountId AND t.deletedAt IS NULL ORDER BY t.date DESC")
    List<Transaction> findAllByUserIdAndAccountIdForReport(@Param("userId") UUID userId, @Param("accountId") UUID accountId);
    
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.category.id = :categoryId AND t.deletedAt IS NULL ORDER BY t.date DESC")
    List<Transaction> findAllByUserIdAndCategoryIdForReport(@Param("userId") UUID userId, @Param("categoryId") UUID categoryId);
    
    @Query("SELECT t FROM Transaction t JOIN FETCH t.category WHERE t.user.id = :userId AND t.type = :type AND t.deletedAt IS NULL ORDER BY t.date DESC")
    List<Transaction> findAllByUserIdAndTypeForReport(@Param("userId") UUID userId, @Param("type") Transaction.TransactionType type);
    
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.date >= :dateFrom AND t.date <= :dateTo AND t.deletedAt IS NULL ORDER BY t.date DESC")
    List<Transaction> findAllByUserIdAndDateRangeForReport(@Param("userId") UUID userId, @Param("dateFrom") LocalDate dateFrom, @Param("dateTo") LocalDate dateTo);
    
    @Query("SELECT t FROM Transaction t JOIN FETCH t.account JOIN FETCH t.category WHERE t.id IN :ids AND t.user.id = :userId AND t.deletedAt IS NULL")
    List<Transaction> findAllByIdsAndUserId(@Param("ids") List<UUID> ids, @Param("userId") UUID userId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.user.id = :userId AND t.category.id = :categoryId " +
           "AND t.type = 'EXPENSE' AND t.date >= :startDate AND t.date <= :endDate AND t.deletedAt IS NULL")
    java.math.BigDecimal sumExpensesByCategoryAndDateRange(
        @Param("userId") UUID userId,
        @Param("categoryId") UUID categoryId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("SELECT t FROM Transaction t JOIN FETCH t.account JOIN FETCH t.category WHERE t.user.id = :userId " +
           "AND t.type = 'EXPENSE' AND t.date >= :startDate AND t.date <= :endDate AND t.deletedAt IS NULL ORDER BY t.date ASC")
    List<Transaction> findUpcomingExpensesByUserId(
        @Param("userId") UUID userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("SELECT t FROM Transaction t JOIN FETCH t.category WHERE t.user.id = :userId AND t.type = :type " +
           "AND LOWER(TRIM(COALESCE(t.description, ''))) = LOWER(:description) AND t.deletedAt IS NULL ORDER BY t.date DESC")
    List<Transaction> findByUserIdAndTypeAndDescriptionExact(
        @Param("userId") UUID userId,
        @Param("type") Transaction.TransactionType type,
        @Param("description") String description
    );

    @Query("SELECT t FROM Transaction t JOIN FETCH t.category WHERE t.user.id = :userId AND t.type = :type " +
           "AND t.description IS NOT NULL AND LOWER(t.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "AND t.deletedAt IS NULL ORDER BY t.date DESC")
    List<Transaction> findByUserIdAndTypeAndDescriptionContaining(
        @Param("userId") UUID userId,
        @Param("type") Transaction.TransactionType type,
        @Param("searchTerm") String searchTerm
    );

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t JOIN t.category c WHERE t.user.id = :userId " +
           "AND t.type = 'EXPENSE' AND t.date >= :startDate AND t.date <= :endDate AND t.deletedAt IS NULL " +
           "AND (LOWER(COALESCE(t.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    java.math.BigDecimal sumExpensesByKeywordAndDateRange(
        @Param("userId") UUID userId,
        @Param("keyword") String keyword,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.user.id = :userId " +
           "AND t.type = 'INCOME' AND t.date >= :startDate AND t.date <= :endDate AND t.deletedAt IS NULL")
    java.math.BigDecimal sumIncomeByDateRange(
        @Param("userId") UUID userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.user.id = :userId " +
           "AND t.type = 'EXPENSE' AND t.date >= :startDate AND t.date <= :endDate AND t.deletedAt IS NULL")
    java.math.BigDecimal sumExpensesByDateRange(
        @Param("userId") UUID userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("SELECT DISTINCT t.date FROM Transaction t WHERE t.user.id = :userId AND t.deletedAt IS NULL AND t.date >= :since ORDER BY t.date DESC")
    List<LocalDate> findDistinctDatesSince(@Param("userId") UUID userId, @Param("since") LocalDate since);
}
