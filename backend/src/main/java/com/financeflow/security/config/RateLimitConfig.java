package com.financeflow.security.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class RateLimitConfig {
    
    @Value("${rate-limit.auth.requests-per-minute:5}")
    private int authRequestsPerMinute;
    
    @Value("${rate-limit.api.requests-per-minute:100}")
    private int apiRequestsPerMinute;
    
    @Value("${rate-limit.auth.block-duration-minutes:15}")
    private int authBlockDurationMinutes;
    
    @Bean("authRateLimitCache")
    public Cache<String, Integer> authRateLimitCache() {
        return Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();
    }
    
    @Bean("apiRateLimitCache")
    public Cache<String, Integer> apiRateLimitCache() {
        return Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(100_000)
            .build();
    }
    
    @Bean("blockedIpsCache")
    public Cache<String, Boolean> blockedIpsCache() {
        return Caffeine.newBuilder()
            .expireAfterWrite(authBlockDurationMinutes, TimeUnit.MINUTES)
            .maximumSize(1_000)
            .build();
    }
}
