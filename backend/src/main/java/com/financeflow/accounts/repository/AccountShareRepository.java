package com.financeflow.accounts.repository;

import com.financeflow.accounts.domain.AccountShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountShareRepository extends JpaRepository<AccountShare, UUID> {

    List<AccountShare> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

    @Query("SELECT s FROM AccountShare s JOIN FETCH s.sharedWithUser WHERE s.account.id = :accountId")
    List<AccountShare> findByAccountIdWithUser(@Param("accountId") UUID accountId);

    @Query("SELECT s FROM AccountShare s JOIN FETCH s.account a JOIN FETCH a.user WHERE s.sharedWithUser.id = :userId AND a.deletedAt IS NULL")
    List<AccountShare> findBySharedWithUserId(@Param("userId") UUID userId);

    Optional<AccountShare> findByAccountIdAndSharedWithUserId(UUID accountId, UUID sharedWithUserId);

    boolean existsByAccountIdAndSharedWithUserId(UUID accountId, UUID sharedWithUserId);

    void deleteByAccountIdAndSharedWithUserId(UUID accountId, UUID sharedWithUserId);
}
