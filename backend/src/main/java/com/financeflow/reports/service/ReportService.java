package com.financeflow.reports.service;

import com.financeflow.transactions.dto.TransactionResponse;
import jakarta.annotation.PostConstruct;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {
    
    private JasperReport compiledReport;
    
    @PostConstruct
    public void init() {
        try {
            // Tentar carregar o .jasper compilado primeiro
            InputStream compiledStream = getClass()
                .getResourceAsStream("/reports/transactions.jasper");
            
            if (compiledStream != null) {
                // Se o .jasper existe, usar ele (mais rápido)
                try (ObjectInputStream ois = new ObjectInputStream(compiledStream)) {
                    compiledReport = (JasperReport) ois.readObject();
                }
            } else {
                // Se não existe, compilar o .jrxml e fazer cache
                InputStream reportStream = getClass()
                    .getResourceAsStream("/reports/transactions.jrxml");
                
                if (reportStream == null) {
                    throw new RuntimeException("Template do relatório não encontrado");
                }
                
                compiledReport = JasperCompileManager.compileReport(reportStream);
                reportStream.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao inicializar relatório: " + e.getMessage(), e);
        }
    }
    
    public byte[] generateTransactionsReport(List<TransactionResponse> transactions) throws JRException {
        if (compiledReport == null) {
            throw new RuntimeException("Relatório não foi inicializado corretamente");
        }
        
        // Preparar os dados - criar uma lista de Map para o dataSource
        List<Map<String, Object>> dataList = transactions.stream()
            .map(t -> {
                Map<String, Object> row = new HashMap<>();
                row.put("description", t.getDescription());
                row.put("date", t.getDate());
                row.put("accountName", t.getAccountName());
                row.put("categoryName", t.getCategoryName());
                row.put("amount", t.getAmount());
                row.put("type", t.getType().name());
                return row;
            })
            .toList();
        
        // Criar dataSource
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(dataList);
        
        // Parâmetros do relatório
        Map<String, Object> parameters = new HashMap<>();
        
        // Preencher o relatório usando o template compilado em cache
        JasperPrint jasperPrint = JasperFillManager.fillReport(
            compiledReport, 
            parameters, 
            dataSource
        );
        
        // Exportar para PDF
        return JasperExportManager.exportReportToPdf(jasperPrint);
    }
}
