package com.financeflow.accounts.mapper;

import com.financeflow.accounts.domain.Account;
import com.financeflow.accounts.dto.AccountResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AccountMapper {
    
    AccountResponse toResponse(Account account);
    
    List<AccountResponse> toResponseList(List<Account> accounts);
}
