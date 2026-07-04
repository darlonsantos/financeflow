package com.financeflow.config.health;

import com.financeflow.transactions.repository.TransactionRepository;
import com.financeflow.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health Indicator customizado para verificar o status da aplicação
 * incluindo contadores de entidades principais.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationHealthIndicator implements HealthIndicator {
    
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    
    @Override
    public Health health() {
        try {
            // Verificar se consegue contar usuários e transações
            long userCount = userRepository.count();
            long transactionCount = transactionRepository.count();
            
            return Health.up()
                    .withDetail("users_count", userCount)
                    .withDetail("transactions_count", transactionCount)
                    .withDetail("status", "Application is running")
                    .build();
                    
        } catch (Exception e) {
            log.error("Application health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withException(e)
                    .build();
        }
    }
}
