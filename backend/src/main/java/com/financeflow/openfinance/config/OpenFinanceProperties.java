package com.financeflow.openfinance.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "open-finance")
@Data
public class OpenFinanceProperties {

    private String provider = "pluggy";
    private String baseUrl = "https://api.pluggy.ai";
    private String clientId = "";
    private String clientSecret = "";
    private boolean enabled = true;
    private String cryptoKey = "financeflow-open-finance-key-32chars";
}
