package com.financeflow.transactions.controller;

import com.financeflow.reports.service.ReportService;
import com.financeflow.transactions.domain.Transaction;
import com.financeflow.transactions.dto.CategorySuggestionRequest;
import com.financeflow.transactions.dto.CategorySuggestionResponse;
import com.financeflow.transactions.dto.TransactionRequest;
import com.financeflow.transactions.dto.TransactionResponse;
import com.financeflow.transactions.service.CategorySuggestionService;
import com.financeflow.transactions.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.JRException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {
    
    private final TransactionService transactionService;
    private final ReportService reportService;
    private final CategorySuggestionService categorySuggestionService;
    
    @GetMapping
    public ResponseEntity<Map<String, Object>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "date,desc") String sort,
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) Transaction.TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        
        Sort.Direction direction = sort.endsWith(",desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        String sortField = sort.split(",")[0];
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        
        Page<TransactionResponse> transactions;
        
        if (accountId != null) {
            transactions = transactionService.findByAccount(accountId, pageable);
        } else if (categoryId != null) {
            transactions = transactionService.findByCategory(categoryId, pageable);
        } else if (type != null) {
            transactions = transactionService.findByType(type, pageable);
        } else if (dateFrom != null && dateTo != null) {
            transactions = transactionService.findByDateRange(dateFrom, dateTo, pageable);
        } else {
            transactions = transactionService.findAll(pageable);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("data", transactions.getContent());
        result.put("pagination", Map.of(
            "page", transactions.getNumber(),
            "size", transactions.getSize(),
            "totalElements", transactions.getTotalElements(),
            "totalPages", transactions.getTotalPages()
        ));
        result.put("message", "Transações listadas com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> findById(@PathVariable UUID id) {
        TransactionResponse transaction = transactionService.findById(id);
        Map<String, Object> result = new HashMap<>();
        result.put("data", transaction);
        result.put("message", "Transação encontrada com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody TransactionRequest request) {
        TransactionResponse transaction = transactionService.create(request);
        Map<String, Object> result = new HashMap<>();
        result.put("data", transaction);
        result.put("message", "Transação criada com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable UUID id,
            @Valid @RequestBody TransactionRequest request) {
        TransactionResponse transaction = transactionService.update(id, request);
        Map<String, Object> result = new HashMap<>();
        result.put("data", transaction);
        result.put("message", "Transação atualizada com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable UUID id) {
        transactionService.delete(id);
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Transação excluída com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }
    
    @DeleteMapping("/batch")
    public ResponseEntity<Map<String, Object>> deleteBatch(@RequestBody List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("message", "Lista de IDs não pode estar vazia");
            result.put("timestamp", java.time.Instant.now().toString());
            return ResponseEntity.badRequest().body(result);
        }
        
        transactionService.deleteBatch(ids);
        Map<String, Object> result = new HashMap<>();
        result.put("message", ids.size() + " transação(ões) excluída(s) com sucesso");
        result.put("deletedCount", ids.size());
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/report")
    public ResponseEntity<byte[]> generateReport(
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) Transaction.TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        
        try {
            // Buscar transações com os filtros aplicados
            List<TransactionResponse> transactions = transactionService.findAllForReport(
                accountId, categoryId, type, dateFrom, dateTo
            );
            
            // Gerar o relatório PDF
            byte[] pdfBytes = reportService.generateTransactionsReport(transactions);
            
            // Configurar headers para download
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "relatorio_transacoes.pdf");
            headers.setContentLength(pdfBytes.length);
            
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (JRException e) {
            throw new RuntimeException("Erro ao gerar relatório: " + e.getMessage(), e);
        }
    }
    
    @PostMapping("/process-recurring")
    public ResponseEntity<Map<String, Object>> processRecurringTransactions() {
        transactionService.processRecurringTransactions();
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Transações recorrentes processadas com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/suggest-category")
    public ResponseEntity<Map<String, Object>> suggestCategory(@Valid @RequestBody CategorySuggestionRequest request) {
        try {
            java.util.Optional<CategorySuggestionResponse> suggestion = categorySuggestionService.suggestCategory(request);
            Map<String, Object> result = new HashMap<>();
            result.put("data", suggestion.orElse(null));
            result.put("message", suggestion.isPresent() ? "Sugestão encontrada" : "Nenhuma sugestão disponível");
            result.put("timestamp", java.time.Instant.now().toString());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Erro ao sugerir categoria: type={}, description={}", request != null ? request.getType() : null,
                request != null ? request.getDescription() : null, e);
            throw e;
        }
    }
}
