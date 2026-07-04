package com.financeflow.accounts.service;

import com.financeflow.accounts.dto.AccountRequest;
import com.financeflow.accounts.dto.AccountResponse;

import java.util.List;
import java.util.UUID;

public interface IAccountService {
    
    List<AccountResponse> findAll();
    
    AccountResponse findById(UUID id);
    
    AccountResponse create(AccountRequest request);
    
    AccountResponse update(UUID id, AccountRequest request);
    
    void delete(UUID id);
    
    AccountResponse getBalance(UUID id);
}
