package com.financeflow.openfinance.repository;

import com.financeflow.openfinance.domain.BankAccountConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BankAccountConnectionRepository extends JpaRepository<BankAccountConnection, UUID> {

    List<BankAccountConnection> findByConnectionId(UUID connectionId);

    Optional<BankAccountConnection> findByConnectionIdAndProviderAccountId(UUID connectionId, String providerAccountId);
}
