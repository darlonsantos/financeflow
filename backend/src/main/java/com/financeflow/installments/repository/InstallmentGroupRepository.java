package com.financeflow.installments.repository;

import com.financeflow.installments.domain.InstallmentGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstallmentGroupRepository extends JpaRepository<InstallmentGroup, UUID>, JpaSpecificationExecutor<InstallmentGroup> {

    @Query("SELECT g FROM InstallmentGroup g JOIN FETCH g.account JOIN FETCH g.category WHERE g.user.id = :userId AND g.deletedAt IS NULL ORDER BY g.firstDueDate DESC")
    Page<InstallmentGroup> findAllByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT g FROM InstallmentGroup g JOIN FETCH g.account JOIN FETCH g.category JOIN FETCH g.items WHERE g.id = :id AND g.user.id = :userId AND g.deletedAt IS NULL")
    Optional<InstallmentGroup> findByIdAndUserIdWithItems(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("SELECT g FROM InstallmentGroup g JOIN FETCH g.account JOIN FETCH g.category WHERE g.id = :id AND g.user.id = :userId AND g.deletedAt IS NULL")
    Optional<InstallmentGroup> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("SELECT g FROM InstallmentGroup g JOIN FETCH g.account JOIN FETCH g.category WHERE g.user.id = :userId AND g.status = 'ACTIVE' AND g.deletedAt IS NULL ORDER BY g.firstDueDate ASC")
    List<InstallmentGroup> findAllActiveByUserId(@Param("userId") UUID userId);
}
