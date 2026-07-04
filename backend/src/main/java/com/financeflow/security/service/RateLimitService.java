package com.financeflow.security.service;

import com.github.benmanes.caffeine.cache.Cache;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {
    
    @Qualifier("authRateLimitCache")
    private final Cache<String, Integer> authRateLimitCache;
    
    @Qualifier("apiRateLimitCache")
    private final Cache<String, Integer> apiRateLimitCache;
    
    @Qualifier("blockedIpsCache")
    private final Cache<String, Boolean> blockedIpsCache;
    
    @Value("${rate-limit.auth.requests-per-minute:5}")
    private int authRequestsPerMinute;
    
    @Value("${rate-limit.api.requests-per-minute:100}")
    private int apiRequestsPerMinute;
    
    public boolean isAllowed(String key, boolean isAuthEndpoint) {
        Cache<String, Integer> cache = isAuthEndpoint ? authRateLimitCache : apiRateLimitCache;
        int limit = isAuthEndpoint ? authRequestsPerMinute : apiRequestsPerMinute;
        
        Integer currentCount = cache.getIfPresent(key);
        if (currentCount == null) {
            cache.put(key, 1);
            return true;
        }
        
        if (currentCount >= limit) {
            if (isAuthEndpoint) {
                blockIp(key);
            }
            log.warn("Rate limit exceeded for key: {}", key);
            return false;
        }
        
        cache.put(key, currentCount + 1);
        return true;
    }
    
    public boolean isBlocked(String ip) {
        return blockedIpsCache.getIfPresent(ip) != null;
    }
    
    private void blockIp(String ip) {
        blockedIpsCache.put(ip, true);
        log.warn("IP blocked due to rate limit: {}", ip);
    }
    
    public String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    public void resetLimit(String key) {
        authRateLimitCache.invalidate(key);
        apiRateLimitCache.invalidate(key);
    }
}
