package com.financeflow.openfinance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConnectBankRequest {

    @NotBlank
    private String provider;
}
