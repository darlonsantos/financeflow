package com.financeflow.categories.controller;

import com.financeflow.categories.domain.Category;
import com.financeflow.categories.dto.CategoryRequest;
import com.financeflow.categories.dto.CategoryResponse;
import com.financeflow.categories.service.CategoryReportService;
import com.financeflow.categories.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.JRException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final CategoryReportService categoryReportService;
    
    @GetMapping
    public ResponseEntity<Map<String, Object>> findAll(
            @RequestParam(required = false) Category.CategoryType type) {
        List<CategoryResponse> categories;
        
        if (type != null) {
            categories = categoryService.findByType(type);
        } else {
            categories = categoryService.findAll();
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("data", categories);
        result.put("message", "Categorias listadas com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> findById(@PathVariable UUID id) {
        CategoryResponse category = categoryService.findById(id);
        Map<String, Object> result = new HashMap<>();
        result.put("data", category);
        result.put("message", "Categoria encontrada com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse category = categoryService.create(request);
        Map<String, Object> result = new HashMap<>();
        result.put("data", category);
        result.put("message", "Categoria criada com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse category = categoryService.update(id, request);
        Map<String, Object> result = new HashMap<>();
        result.put("data", category);
        result.put("message", "Categoria atualizada com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable UUID id) {
        categoryService.delete(id);
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Categoria excluída com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/report")
    public ResponseEntity<byte[]> generateReport(
            @RequestParam(required = false) Category.CategoryType type) {
        try {
            List<CategoryResponse> categories = type != null
                    ? categoryService.findByType(type)
                    : categoryService.findAll();

            byte[] pdfBytes = categoryReportService.generateCategoriesReport(categories);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "relatorio_categorias.pdf");
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (JRException e) {
            throw new RuntimeException("Erro ao gerar relatório: " + e.getMessage(), e);
        }
    }
}
