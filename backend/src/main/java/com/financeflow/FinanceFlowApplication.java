package com.financeflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableCaching
public class FinanceFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceFlowApplication.class, args);
    }
}
 