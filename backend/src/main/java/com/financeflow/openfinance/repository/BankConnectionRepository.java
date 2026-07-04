package com.financeflow.openfinance.repository;

import com.financeflow.openfinance.domain.BankConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BankConnectionRepository extends JpaRepository<BankConnection, UUID> {

    List<BankConnection> findByUserIdOrderByDataCriacaoDesc(UUID userId);

    Optional<BankConnection> findByIdAndUserId(UUID id, UUID userId);

    List<BankConnection> findByStatus(BankConnection.Status status);
}
