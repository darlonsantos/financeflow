package com.financeflow.config.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Serviço para registrar métricas customizadas da aplicação
 */
@Component
@Slf4j
public class MetricsService {
    
    private final MeterRegistry meterRegistry;
    
    // Contadores
    private final Counter transactionCreatedCounter;
    private final Counter transactionUpdatedCounter;
    private final Counter transactionDeletedCounter;
    private final Counter authSuccessCounter;
    private final Counter authFailureCounter;
    private final Counter rateLimitExceededCounter;
    
    // Timers
    private final Timer transactionCreationTimer;
    private final Timer transactionQueryTimer;
    
    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Inicializar contadores
        this.transactionCreatedCounter = Counter.builder("transactions.created")
                .description("Number of transactions created")
                .tag("type", "counter")
                .register(meterRegistry);
        
        this.transactionUpdatedCounter = Counter.builder("transactions.updated")
                .description("Number of transactions updated")
                .tag("type", "counter")
                .register(meterRegistry);
        
        this.transactionDeletedCounter = Counter.builder("transactions.deleted")
                .description("Number of transactions deleted")
                .tag("type", "counter")
                .register(meterRegistry);
        
        this.authSuccessCounter = Counter.builder("auth.success")
                .description("Number of successful authentication attempts")
                .tag("type", "counter")
                .register(meterRegistry);
        
        this.authFailureCounter = Counter.builder("auth.failure")
                .description("Number of failed authentication attempts")
                .tag("type", "counter")
                .register(meterRegistry);
        
        this.rateLimitExceededCounter = Counter.builder("ratelimit.exceeded")
                .description("Number of rate limit exceeded events")
                .tag("type", "counter")
                .register(meterRegistry);
        
        // Inicializar timers
        this.transactionCreationTimer = Timer.builder("transactions.creation.time")
                .description("Time taken to create a transaction")
                .tag("type", "timer")
                .register(meterRegistry);
        
        this.transactionQueryTimer = Timer.builder("transactions.query.time")
                .description("Time taken to query transactions")
                .tag("type", "timer")
                .register(meterRegistry);
    }
    
    // Métodos para incrementar contadores
    public void incrementTransactionCreated() {
        transactionCreatedCounter.increment();
        log.debug("Transaction created counter incremented");
    }
    
    public void incrementTransactionUpdated() {
        transactionUpdatedCounter.increment();
        log.debug("Transaction updated counter incremented");
    }
    
    public void incrementTransactionDeleted() {
        transactionDeletedCounter.increment();
        log.debug("Transaction deleted counter incremented");
    }
    
    public void incrementAuthSuccess() {
        authSuccessCounter.increment();
        log.debug("Auth success counter incremented");
    }
    
    public void incrementAuthFailure() {
        authFailureCounter.increment();
        log.debug("Auth failure counter incremented");
    }
    
    public void incrementRateLimitExceeded() {
        rateLimitExceededCounter.increment();
        log.debug("Rate limit exceeded counter incremented");
    }
    
    // Métodos para registrar tempo de execução
    public void recordTransactionCreationTime(long milliseconds) {
        transactionCreationTimer.record(milliseconds, java.util.concurrent.TimeUnit.MILLISECONDS);
        log.debug("Transaction creation time recorded: {}ms", milliseconds);
    }
    
    public void recordTransactionQueryTime(long milliseconds) {
        transactionQueryTimer.record(milliseconds, java.util.concurrent.TimeUnit.MILLISECONDS);
        log.debug("Transaction query time recorded: {}ms", milliseconds);
    }
    
    // Métodos utilitários para timing
    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void stopTimer(Timer.Sample sample, Timer timer) {
        sample.stop(timer);
    }
    
    // Getter para usar com @Timed annotation
    public Timer getTransactionCreationTimer() {
        return transactionCreationTimer;
    }
    
    public Timer getTransactionQueryTimer() {
        return transactionQueryTimer;
    }
}
