package com.financeflow.automation.repository;

import com.financeflow.automation.domain.AutomationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AutomationRuleRepository extends JpaRepository<AutomationRule, UUID> {

    @Query("SELECT r FROM AutomationRule r WHERE r.user.id = :userId ORDER BY r.createdAt DESC")
    List<AutomationRule> findAllByUserId(@Param("userId") UUID userId);

    @Query("SELECT r FROM AutomationRule r WHERE r.user.id = :userId AND r.active = true AND r.conditionType = :conditionType")
    List<AutomationRule> findActiveByUserIdAndConditionType(
            @Param("userId") UUID userId,
            @Param("conditionType") String conditionType);

    @Query("SELECT r FROM AutomationRule r WHERE r.id = :id AND r.user.id = :userId")
    java.util.Optional<AutomationRule> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);
}
