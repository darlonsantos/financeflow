package com.financeflow.transactions.service;

import com.financeflow.transactions.domain.Transaction;
import com.financeflow.transactions.dto.TransactionRequest;
import com.financeflow.transactions.dto.TransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ITransactionService {
    
    Page<TransactionResponse> findAll(Pageable pageable);
    
    Page<TransactionResponse> findByAccount(UUID accountId, Pageable pageable);
    
    Page<TransactionResponse> findByCategory(UUID categoryId, Pageable pageable);
    
    Page<TransactionResponse> findByType(Transaction.TransactionType type, Pageable pageable);
    
    Page<TransactionResponse> findByDateRange(LocalDate startDate, LocalDate endDate, Pageable pageable);
    
    List<TransactionResponse> findAllForReport(UUID accountId, UUID categoryId, 
                                               Transaction.TransactionType type, 
                                               LocalDate dateFrom, LocalDate dateTo);
    
    TransactionResponse findById(UUID id);
    
    TransactionResponse create(TransactionRequest request);
    
    TransactionResponse update(UUID id, TransactionRequest request);
    
    void delete(UUID id);
    
    void deleteBatch(List<UUID> ids);
    
    void recalculateAccountBalance(UUID accountId);
    
    void processRecurringTransactions();
}
