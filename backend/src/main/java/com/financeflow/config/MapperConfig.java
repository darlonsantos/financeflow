package com.financeflow.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "com.financeflow")
public class MapperConfig {
    // Esta classe garante que o Spring escaneie todos os pacotes incluindo os mappers gerados pelo MapStruct
}