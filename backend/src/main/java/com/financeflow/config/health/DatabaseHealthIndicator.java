package com.financeflow.config.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Health Indicator customizado para verificar o status do banco de dados
 * incluindo latência e disponibilidade.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseHealthIndicator implements HealthIndicator {
    
    private final DataSource dataSource;
    
    @Override
    public Health health() {
        try {
            long startTime = System.currentTimeMillis();
            
            // Testar conexão e executar query simples
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT 1")) {
                
                if (!resultSet.next()) {
                    return Health.down()
                            .withDetail("error", "Query test failed")
                            .build();
                }
                
                long latency = System.currentTimeMillis() - startTime;
                
                // Verificar se latência está aceitável
                if (latency > 1000) {
                    return Health.status("DEGRADED")
                            .withDetail("latency_ms", latency)
                            .withDetail("message", "Database responding slowly")
                            .build();
                }
                
                return Health.up()
                        .withDetail("database", "PostgreSQL")
                        .withDetail("latency_ms", latency)
                        .withDetail("status", "Healthy")
                        .build();
            }
            
        } catch (Exception e) {
            log.error("Database health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withException(e)
                    .build();
        }
    }
}
