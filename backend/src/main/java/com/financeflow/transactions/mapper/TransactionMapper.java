package com.financeflow.transactions.mapper;

import com.financeflow.transactions.domain.Transaction;
import com.financeflow.transactions.dto.TransactionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TransactionMapper {
    
    @Mapping(target = "accountId", source = "account.id")
    @Mapping(target = "accountName", source = "account.name")
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "currencyCode", source = "account.currencyCode")
    TransactionResponse toResponse(Transaction transaction);
    
    List<TransactionResponse> toResponseList(List<Transaction> transactions);
}
