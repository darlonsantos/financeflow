package com.financeflow.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financeflow.security.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {
    
    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) 
            throws ServletException, IOException {
        
        String ip = rateLimitService.getClientIp(request);
        String path = request.getRequestURI();
        boolean isAuthEndpoint = path.startsWith("/api/v1/auth/");
        
        // Verificar se IP está bloqueado
        if (rateLimitService.isBlocked(ip)) {
            sendRateLimitError(response, "IP bloqueado devido a muitas tentativas. Tente novamente mais tarde.");
            return;
        }
        
        // Verificar rate limit
        String rateLimitKey = isAuthEndpoint ? ip : ip + ":" + path;
        if (!rateLimitService.isAllowed(rateLimitKey, isAuthEndpoint)) {
            sendRateLimitError(response, "Muitas requisições. Tente novamente em alguns instantes.");
            return;
        }
        
        filterChain.doFilter(request, response);
    }
    
    private void sendRateLimitError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        Map<String, Object> error = new HashMap<>();
        error.put("error", Map.of(
            "code", "RATE_LIMIT_EXCEEDED",
            "message", message
        ));
        error.put("timestamp", Instant.now().toString());
        
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
