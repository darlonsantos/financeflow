package com.financeflow.openfinance.repository;

import com.financeflow.openfinance.domain.ImportedTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ImportedTransactionRepository extends JpaRepository<ImportedTransaction, UUID> {

    Optional<ImportedTransaction> findByAccountIdAndProviderTransactionId(UUID accountId, String providerTransactionId);

    boolean existsByHashUnico(String hashUnico);

    List<ImportedTransaction> findByAccountConnectionIdOrderByDataTransacaoDesc(UUID connectionId);

    Optional<ImportedTransaction> findTopByAccountConnectionIdOrderByDataTransacaoDesc(UUID connectionId);
}
