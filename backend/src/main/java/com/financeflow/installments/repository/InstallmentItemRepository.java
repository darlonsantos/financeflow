package com.financeflow.installments.repository;

import com.financeflow.installments.domain.InstallmentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstallmentItemRepository extends JpaRepository<InstallmentItem, UUID> {

    List<InstallmentItem> findByInstallmentGroupIdOrderByInstallmentNumberAsc(UUID installmentGroupId);

    @Query("SELECT i FROM InstallmentItem i JOIN FETCH i.installmentGroup g JOIN FETCH g.account JOIN FETCH g.category WHERE i.id = :id AND g.user.id = :userId")
    Optional<InstallmentItem> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("SELECT i FROM InstallmentItem i JOIN i.installmentGroup g WHERE g.user.id = :userId AND i.status = 'PENDING' AND g.deletedAt IS NULL AND g.status = 'ACTIVE' AND i.dueDate BETWEEN :from AND :to ORDER BY i.dueDate ASC")
    List<InstallmentItem> findPendingByUserIdAndDueDateBetween(
        @Param("userId") UUID userId,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to
    );
}
