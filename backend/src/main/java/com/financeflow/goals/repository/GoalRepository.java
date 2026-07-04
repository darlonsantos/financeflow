package com.financeflow.goals.repository;

import com.financeflow.goals.domain.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GoalRepository extends JpaRepository<Goal, UUID> {

    @Query("SELECT g FROM Goal g WHERE g.user.id = :userId AND g.deletedAt IS NULL " +
           "AND g.status = 'ACTIVE' AND g.dueDate IS NOT NULL AND g.dueDate BETWEEN :startDate AND :endDate")
    List<Goal> findAllActiveByUserIdAndDueDateBetween(
        @Param("userId") UUID userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("SELECT g FROM Goal g WHERE g.user.id = :userId AND g.deletedAt IS NULL ORDER BY g.status, g.dueDate ASC")
    List<Goal> findAllByUserId(@Param("userId") UUID userId);

    @Query("SELECT g FROM Goal g WHERE g.id = :id AND g.user.id = :userId AND g.deletedAt IS NULL")
    Optional<Goal> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);
}
