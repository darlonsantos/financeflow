package com.financeflow.categories.service;

import com.financeflow.categories.domain.Category;
import com.financeflow.categories.dto.CategoryResponse;
import jakarta.annotation.PostConstruct;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CategoryReportService {

    private static final DateTimeFormatter EMISSION_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private JasperReport compiledReport;

    @PostConstruct
    public void init() {
        try {
            InputStream compiledStream = getClass()
                    .getResourceAsStream("/reports/categories.jasper");

            if (compiledStream != null) {
                try (ObjectInputStream ois = new ObjectInputStream(compiledStream)) {
                    compiledReport = (JasperReport) ois.readObject();
                }
            } else {
                InputStream reportStream = getClass()
                        .getResourceAsStream("/reports/categories.jrxml");

                if (reportStream == null) {
                    throw new RuntimeException("Template do relatório de categorias não encontrado");
                }

                compiledReport = JasperCompileManager.compileReport(reportStream);
                reportStream.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao inicializar relatório de categorias: " + e.getMessage(), e);
        }
    }

    public byte[] generateCategoriesReport(List<CategoryResponse> categories) throws JRException {
        if (compiledReport == null) {
            throw new RuntimeException("Relatório de categorias não foi inicializado corretamente");
        }

        List<Map<String, Object>> dataList = categories.stream()
                .map(c -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("name", c.getName());
                    row.put("typeLabel", c.getType() == Category.CategoryType.INCOME ? "Receita" : "Despesa");
                    return row;
                })
                .toList();

        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(dataList);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("emissionDateTime", LocalDateTime.now().format(EMISSION_FORMATTER));

        JasperPrint jasperPrint = JasperFillManager.fillReport(compiledReport, parameters, dataSource);

        return JasperExportManager.exportReportToPdf(jasperPrint);
    }
}