package com.financeflow.installments.repository;

import com.financeflow.installments.domain.InstallmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InstallmentHistoryRepository extends JpaRepository<InstallmentHistory, UUID> {

    List<InstallmentHistory> findByInstallmentGroupIdOrderByCreatedAtAsc(UUID installmentGroupId);
}
