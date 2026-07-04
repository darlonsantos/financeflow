package com.financeflow.openfinance.client;

import java.time.LocalDateTime;

public record ProviderSessionResponse(
    String linkToken,
    String providerConnectionId,
    String accessToken,
    String refreshToken,
    LocalDateTime expiresAt
) {}
