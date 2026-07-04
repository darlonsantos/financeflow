package com.financeflow.assistant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ollama")
public class OllamaProperties {

    private boolean enabled = false;
    private String baseUrl = "http://localhost:11434";
    private String model = "llama3.2";
    private int timeoutSeconds = 30;
}
