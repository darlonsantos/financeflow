package com.financeflow.accounts.dto;

import com.financeflow.accounts.domain.AccountShare;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountShareResponse {

    private UUID id;
    private UUID accountId;
    private UUID sharedWithUserId;
    private String sharedWithUserName;
    private String sharedWithUserEmail;
    private AccountShare.Permission permission;
    private LocalDateTime createdAt;
}
