package com.financeflow.openfinance.scheduler;

import com.financeflow.openfinance.service.OpenFinanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenFinanceScheduler {

    private final OpenFinanceService openFinanceService;

    @Scheduled(cron = "0 30 2 * * *", zone = "America/Sao_Paulo")
    public void syncDaily() {
        try {
            log.info("Iniciando sincronização automática Open Finance.");
            openFinanceService.syncAllActiveConnections();
            log.info("Sincronização automática Open Finance concluída.");
        } catch (Exception e) {
            log.error("Erro na sincronização automática Open Finance.", e);
        }
    }
}
