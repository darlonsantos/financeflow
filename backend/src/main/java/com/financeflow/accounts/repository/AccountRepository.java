package com.financeflow.accounts.repository;

import com.financeflow.accounts.domain.Account;
import com.financeflow.accounts.domain.AccountShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {
    
    @Query("SELECT a FROM Account a WHERE a.user.id = :userId AND a.deletedAt IS NULL")
    List<Account> findAllByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT a FROM Account a WHERE a.id = :id AND a.user.id = :userId AND a.deletedAt IS NULL")
    Optional<Account> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);
    
    @Query("SELECT a FROM Account a LEFT JOIN AccountShare s ON s.account = a AND s.sharedWithUser.id = :userId " +
           "WHERE a.id = :id AND a.deletedAt IS NULL AND (a.user.id = :userId OR s.id IS NOT NULL)")
    Optional<Account> findByIdAccessibleByUser(@Param("id") UUID id, @Param("userId") UUID userId);
    
    @Query("SELECT COUNT(a) > 0 FROM Account a WHERE a.id = :id AND a.user.id = :userId AND a.deletedAt IS NULL")
    boolean existsByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);
}
