package com.financeflow.openfinance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConfirmConnectionRequest {

    @NotBlank
    private String providerConnectionId;
}
