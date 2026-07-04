package com.financeflow.email;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "email")
@Data
public class EmailProperties {
    
    private String from;
    private String frontendUrl;
    private PasswordReset passwordReset = new PasswordReset();
    private EmailVerification emailVerification = new EmailVerification();
    
    @Data
    public static class PasswordReset {
        private int expirationHours = 24;
    }
    
    @Data
    public static class EmailVerification {
        private int expirationHours = 72;
    }
}
