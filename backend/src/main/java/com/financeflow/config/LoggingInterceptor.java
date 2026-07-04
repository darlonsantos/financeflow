package com.financeflow.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class LoggingInterceptor implements HandlerInterceptor {
    
    private static final String REQUEST_ID = "requestId";
    private static final String USER_ID = "userId";
    private static final String HTTP_METHOD = "httpMethod";
    private static final String REQUEST_URI = "requestUri";
    private static final String CLIENT_IP = "clientIp";
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Gerar ID único para a requisição
        String requestId = UUID.randomUUID().toString();
        MDC.put(REQUEST_ID, requestId);
        MDC.put(HTTP_METHOD, request.getMethod());
        MDC.put(REQUEST_URI, request.getRequestURI());
        MDC.put(CLIENT_IP, getClientIp(request));
        
        // Adicionar user ID se autenticado
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UUID) {
            UUID userId = (UUID) authentication.getPrincipal();
            MDC.put(USER_ID, userId.toString());
        }
        
        // Adicionar request ID ao header da resposta
        response.setHeader("X-Request-ID", requestId);
        
        log.info("Request received", Map.of(
            "method", request.getMethod(),
            "uri", request.getRequestURI(),
            "ip", getClientIp(request)
        ));
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                               Object handler, Exception ex) {
        if (ex != null) {
            log.error("Request failed", Map.of(
                "status", response.getStatus(),
                "error", ex.getMessage()
            ), ex);
        } else {
            log.info("Request completed", Map.of(
                "status", response.getStatus()
            ));
        }
        
        // Limpar MDC
        MDC.clear();
    }
    
    private String getClientIp(HttpServletRequest request) {
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
}
