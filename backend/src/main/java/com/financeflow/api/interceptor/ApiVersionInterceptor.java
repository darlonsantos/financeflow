package com.financeflow.api.interceptor;

import com.financeflow.api.config.ApiVersionConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor para adicionar headers de versionamento da API nas respostas.
 */
@Component
@RequiredArgsConstructor
public class ApiVersionInterceptor implements HandlerInterceptor {
    
    private final ApiVersionConfig apiVersionConfig;
    
    private static final String API_VERSION_HEADER = "X-API-Version";
    private static final String API_VERSION_DEPRECATED_HEADER = "X-API-Version-Deprecated";
    private static final String API_VERSION_SUNSET_HEADER = "X-API-Version-Sunset";
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestPath = request.getRequestURI();
        
        // Extrair versão do path (/api/v1/...)
        String version = extractVersionFromPath(requestPath);
        
        if (version != null) {
            // Adicionar header com versão usada
            response.setHeader(API_VERSION_HEADER, version);
            
            // Verificar se versão está deprecada
            if (isDeprecatedVersion(version)) {
                response.setHeader(API_VERSION_DEPRECATED_HEADER, "true");
                response.setHeader(API_VERSION_SUNSET_HEADER, getSunsetDate(version));
            }
            
            // Adicionar header com versão mais recente se não for a atual
            if (!version.equals(apiVersionConfig.getLatestVersion())) {
                response.setHeader("X-API-Version-Latest", apiVersionConfig.getLatestVersion());
            }
        } else {
            // Se não especificou versão, usar versão padrão
            response.setHeader(API_VERSION_HEADER, apiVersionConfig.getDefaultVersion());
        }
        
        return true;
    }
    
    private String extractVersionFromPath(String path) {
        if (path.startsWith("/api/v")) {
            String[] parts = path.split("/");
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].startsWith("v") && parts[i].length() > 1) {
                    return parts[i].substring(1); // Remove o "v" do início
                }
            }
        }
        return null;
    }
    
    private boolean isDeprecatedVersion(String version) {
        // Versão v1 não está deprecada ainda
        // Quando v2 for lançada, v1 pode ser marcada como deprecated
        return false; // Implementar lógica quando necessário
    }
    
    private String getSunsetDate(String version) {
        // Data de sunset para versões deprecadas
        // Formato: RFC 7231 date format
        return null; // Implementar quando necessário
    }
}
