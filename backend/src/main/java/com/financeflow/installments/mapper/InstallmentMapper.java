package com.financeflow.installments.mapper;

import com.financeflow.installments.domain.InstallmentGroup;
import com.financeflow.installments.domain.InstallmentItem;
import com.financeflow.installments.dto.InstallmentGroupResponse;
import com.financeflow.installments.dto.InstallmentItemResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface InstallmentMapper {

    @Mapping(target = "accountId", source = "account.id")
    @Mapping(target = "accountName", source = "account.name")
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "currencyCode", source = "account.currencyCode")
    @Mapping(target = "paidAmount", ignore = true)
    @Mapping(target = "remainingAmount", ignore = true)
    @Mapping(target = "paidCount", ignore = true)
    @Mapping(target = "pendingCount", ignore = true)
    @Mapping(target = "items", ignore = true)
    InstallmentGroupResponse toResponse(InstallmentGroup group);

    @Mapping(target = "installmentGroupId", source = "installmentGroup.id")
    @Mapping(target = "transactionId", source = "transaction.id")
    InstallmentItemResponse toItemResponse(InstallmentItem item);

    default InstallmentGroupResponse toResponseWithItems(InstallmentGroup group) {
        InstallmentGroupResponse r = toResponse(group);
        if (group.getItems() != null) {
            r.setItems(group.getItems().stream().map(this::toItemResponse).collect(Collectors.toList()));
            BigDecimal paid = group.getItems().stream()
                .filter(i -> i.getStatus() == InstallmentItem.InstallmentItemStatus.PAID)
                .map(InstallmentItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal pendingSum = group.getItems().stream()
                .filter(i -> i.getStatus() == InstallmentItem.InstallmentItemStatus.PENDING)
                .map(InstallmentItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            long paidCount = group.getItems().stream().filter(i -> i.getStatus() == InstallmentItem.InstallmentItemStatus.PAID).count();
            long pendingCount = group.getItems().stream().filter(i -> i.getStatus() == InstallmentItem.InstallmentItemStatus.PENDING).count();
            r.setPaidAmount(paid);
            r.setRemainingAmount(pendingSum);
            r.setPaidCount((int) paidCount);
            r.setPendingCount((int) pendingCount);
        } else {
            r.setItems(List.of());
            r.setPaidAmount(BigDecimal.ZERO);
            r.setRemainingAmount(group.getTotalAmount());
            r.setPaidCount(0);
            r.setPendingCount(group.getNumberOfInstallments());
        }
        return r;
    }
}
