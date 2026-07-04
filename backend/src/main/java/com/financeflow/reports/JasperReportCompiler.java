package com.financeflow.reports;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;

import java.io.File;

/**
 * Utilitário para compilar arquivos .jrxml para .jasper durante o build.
 * Este arquivo é executado durante o build do Docker/Maven.
 * 
 * Como funciona:
 * 1. Durante o build do Docker, após 'mvn compile', este utilitário é executado
 * 2. Ele compila o .jrxml para .jasper e coloca em target/classes/reports/
 * 3. O .jasper compilado é incluído no JAR final
 * 4. Em runtime, o ReportService tenta carregar o .jasper primeiro (mais rápido)
 * 5. Se o .jasper não existir, compila o .jrxml em tempo de execução
 */
public class JasperReportCompiler {
    
    public static void main(String[] args) {
        try {
            // Obter o diretório de trabalho atual
            String workingDir = System.getProperty("user.dir");
            
            // Caminhos relativos aos recursos
            String sourceDir = workingDir + "/src/main/resources/reports";
            String targetDir = workingDir + "/target/classes/reports";
            
            // Criar diretório de destino se não existir
            File targetDirFile = new File(targetDir);
            if (!targetDirFile.exists()) {
                boolean created = targetDirFile.mkdirs();
                if (!created) {
                    System.err.println("Não foi possível criar o diretório: " + targetDir);
                    return;
                }
            }
            
            // Compilar transactions.jrxml
            String jrxmlFile = sourceDir + "/transactions.jrxml";
            String jasperFile = targetDir + "/transactions.jasper";
            
            File jrxml = new File(jrxmlFile);
            if (!jrxml.exists()) {
                System.err.println("Arquivo não encontrado: " + jrxml.getAbsolutePath());
                System.err.println("Tentando caminho alternativo...");
                // Tentar caminho alternativo
                jrxmlFile = workingDir + "/backend/src/main/resources/reports/transactions.jrxml";
                jasperFile = workingDir + "/backend/target/classes/reports/transactions.jasper";
                jrxml = new File(jrxmlFile);
                if (!jrxml.exists()) {
                    System.err.println("Arquivo não encontrado em nenhum caminho. Pulando compilação.");
                    return;
                }
            }
            
            System.out.println("Compilando relatório JasperReports:");
            System.out.println("  Origem: " + jrxmlFile);
            System.out.println("  Destino: " + jasperFile);
            
            JasperCompileManager.compileReportToFile(jrxmlFile, jasperFile);
            
            System.out.println("✓ Compilação concluída com sucesso!");
            System.out.println("  Arquivo .jasper criado em: " + jasperFile);
            
        } catch (JRException e) {
            System.err.println("Erro ao compilar relatório: " + e.getMessage());
            e.printStackTrace();
            // Não falhar o build se a compilação do relatório falhar
            // O sistema pode compilar em runtime se necessário
            System.exit(0);
        } catch (Exception e) {
            System.err.println("Erro inesperado: " + e.getMessage());
            e.printStackTrace();
            System.exit(0);
        }
    }
}
