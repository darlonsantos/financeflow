package com.financeflow.taxacambio.scheduler;

import com.financeflow.taxacambio.service.TaxaCambioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaxaCambioScheduler {

    private final TaxaCambioService taxaCambioService;

    /** Executa diariamente às 18h (horário de Brasília) para capturar PTAX do dia. */
    @Scheduled(cron = "0 0 18 * * *", zone = "America/Sao_Paulo")
    public void sincronizarTaxaCambio() {
        try {
            log.info("Iniciando sincronização diária de taxa de câmbio (PTAX).");
            taxaCambioService.sincronizarDiario();
            log.info("Sincronização diária de taxa de câmbio concluída.");
        } catch (Exception e) {
            log.error("Erro na sincronização diária de taxa de câmbio.", e);
        }
    }
}
