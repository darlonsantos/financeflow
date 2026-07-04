package com.financeflow.api.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * Configuração de versionamento da API.
 * Gerencia versões disponíveis e versão padrão.
 */
@Configuration
@Getter
public class ApiVersionConfig {
    
    @Value("${api.version.current:v1}")
    private String currentVersion;
    
    @Value("${api.version.default:v1}")
    private String defaultVersion;
    
    @Value("${api.version.supported:v1}")
    private String supportedVersions;
    
    /**
     * Lista de versões suportadas.
     */
    public List<String> getSupportedVersionsList() {
        return Arrays.asList(supportedVersions.split(","));
    }
    
    /**
     * Verifica se uma versão é suportada.
     */
    public boolean isVersionSupported(String version) {
        return getSupportedVersionsList().contains(version);
    }
    
    /**
     * Versão mais recente disponível.
     */
    public String getLatestVersion() {
        return getSupportedVersionsList().stream()
            .max(String::compareTo)
            .orElse(defaultVersion);
    }
}
