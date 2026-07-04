package com.financeflow.audit.service;

import com.financeflow.audit.domain.AuditLog;
import com.financeflow.audit.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {
    
    private final AuditLogRepository auditLogRepository;
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(
            UUID userId,
            String action,
            String entityType,
            UUID entityId,
            Map<String, Object> details) {
        
        try {
            HttpServletRequest request = getCurrentRequest();
            String ipAddress = getClientIp(request);
            String userAgent = request != null ? request.getHeader("User-Agent") : null;
            
            AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
            
            auditLogRepository.save(auditLog);
            
            log.debug("Audit log created", Map.of(
                "userId", userId,
                "action", action,
                "entityType", entityType,
                "entityId", entityId != null ? entityId.toString() : "null"
            ));
        } catch (Exception e) {
            // Não falhar a operação principal se a auditoria falhar
            log.error("Error creating audit log", Map.of(
                "userId", userId,
                "action", action,
                "error", e.getMessage()
            ), e);
        }
    }
    
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
    
    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        
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
