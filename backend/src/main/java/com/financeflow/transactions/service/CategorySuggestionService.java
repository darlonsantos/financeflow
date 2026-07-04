package com.financeflow.transactions.service;

import com.financeflow.categories.domain.Category;
import com.financeflow.categories.repository.CategoryRepository;
import com.financeflow.transactions.domain.Transaction;
import com.financeflow.transactions.dto.CategorySuggestionRequest;
import com.financeflow.transactions.dto.CategorySuggestionResponse;
import com.financeflow.transactions.dto.CategorySuggestionResponse.SuggestionSource;
import com.financeflow.transactions.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço de categorização automática que sugere categorias com base em:
 * - Histórico do usuário (transações anteriores com mesma/similar descrição)
 * - Padrões recorrentes
 * - Palavras-chave na descrição
 * - Categoria mais usada pelo usuário
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CategorySuggestionService {

    private static final int MIN_DESCRIPTION_LENGTH = 2;

    /** Mapeamento de palavras-chave (lowercase) para sugestões de nome de categoria */
    private static final Map<String, List<String>> KEYWORD_TO_CATEGORY_NAMES = Map.ofEntries(
        // Alimentação
        Map.entry("mercado", List.of("Alimentação", "Supermercado", "Compras", "Mercado")),
        Map.entry("supermercado", List.of("Alimentação", "Supermercado", "Compras")),
        Map.entry("padaria", List.of("Alimentação", "Padaria")),
        Map.entry("restaurante", List.of("Alimentação", "Restaurante", "Almoço")),
        Map.entry("lanche", List.of("Alimentação", "Lanche")),
        Map.entry("ifood", List.of("Alimentação", "Restaurante", "Delivery")),
        Map.entry("uber eats", List.of("Alimentação", "Restaurante", "Delivery")),
        Map.entry("rappi", List.of("Alimentação", "Restaurante", "Delivery")),
        Map.entry("açaí", List.of("Alimentação")),
        Map.entry("café", List.of("Alimentação", "Café")),
        // Transporte
        Map.entry("uber", List.of("Transporte", "Uber")),
        Map.entry("99", List.of("Transporte")),
        Map.entry("gasolina", List.of("Transporte", "Combustível")),
        Map.entry("posto", List.of("Transporte", "Combustível")),
        Map.entry("estacionamento", List.of("Transporte")),
        Map.entry("pedágio", List.of("Transporte")),
        Map.entry("ônibus", List.of("Transporte")),
        Map.entry("metrô", List.of("Transporte")),
        // Entretenimento
        Map.entry("netflix", List.of("Entretenimento", "Streaming")),
        Map.entry("spotify", List.of("Entretenimento", "Streaming", "Música")),
        Map.entry("disney", List.of("Entretenimento", "Streaming")),
        Map.entry("amazon prime", List.of("Entretenimento", "Streaming")),
        Map.entry("cinema", List.of("Entretenimento", "Cinema")),
        Map.entry("show", List.of("Entretenimento")),
        Map.entry("ingresso", List.of("Entretenimento")),
        // Contas e Serviços
        Map.entry("luz", List.of("Contas", "Energia", "Utilidades")),
        Map.entry("água", List.of("Contas", "Água", "Utilidades")),
        Map.entry("enel", List.of("Contas", "Energia")),
        Map.entry("internet", List.of("Contas", "Internet")),
        Map.entry("celular", List.of("Contas", "Telefone")),
        Map.entry("vivo", List.of("Contas", "Telefone")),
        Map.entry("claro", List.of("Contas", "Telefone")),
        Map.entry("oi", List.of("Contas", "Telefone")),
        Map.entry("tim", List.of("Contas", "Telefone")),
        Map.entry("aluguel", List.of("Moradia", "Aluguel")),
        Map.entry("condomínio", List.of("Moradia", "Condomínio")),
        Map.entry("iptu", List.of("Moradia", "Impostos")),
        // Saúde
        Map.entry("farmácia", List.of("Saúde", "Farmácia")),
        Map.entry("drogaria", List.of("Saúde", "Farmácia")),
        Map.entry("hospital", List.of("Saúde")),
        Map.entry("clínica", List.of("Saúde")),
        Map.entry("médico", List.of("Saúde")),
        Map.entry("consulta", List.of("Saúde")),
        Map.entry("plano de saúde", List.of("Saúde", "Plano de Saúde")),
        // Educação
        Map.entry("faculdade", List.of("Educação")),
        Map.entry("universidade", List.of("Educação")),
        Map.entry("curso", List.of("Educação")),
        Map.entry("escola", List.of("Educação")),
        Map.entry("livro", List.of("Educação")),
        // Receitas
        Map.entry("salário", List.of("Salário", "Receita")),
        Map.entry("pagamento", List.of("Receita")),
        Map.entry("transferência", List.of("Transferência")),
        Map.entry("pix", List.of("Transferência", "PIX")),
        Map.entry("freelance", List.of("Receita", "Freelance")),
        Map.entry("venda", List.of("Receita"))
    );

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public Optional<CategorySuggestionResponse> suggestCategory(CategorySuggestionRequest request) {
        if (request == null || request.getType() == null) {
            return Optional.empty();
        }
        UUID userId = getCurrentUserId();
        String description = request.getDescription();
        Transaction.TransactionType type = request.getType();

        if (description == null || description.trim().length() < MIN_DESCRIPTION_LENGTH) {
            return Optional.empty();
        }

        String normalizedDesc = description.trim();
        List<Category> userCategories = categoryRepository.findAllByUserIdAndType(userId, toCategoryType(type));

        // 1. Histórico exato (maior confiança)
        Optional<CategorySuggestionResponse> exact = suggestFromExactHistory(userId, type, normalizedDesc);
        if (exact.isPresent()) {
            return exact;
        }

        // 2. Histórico similar (descrição contém)
        Optional<CategorySuggestionResponse> similar = suggestFromSimilarHistory(userId, type, normalizedDesc);
        if (similar.isPresent()) {
            return similar;
        }

        // 3. Palavras-chave
        Optional<CategorySuggestionResponse> keyword = suggestFromKeywords(normalizedDesc, userCategories);
        if (keyword.isPresent()) {
            return keyword;
        }

        // 4. Categoria mais usada
        return suggestMostUsedCategory(userId, type);
    }

    private Optional<CategorySuggestionResponse> suggestFromExactHistory(
            UUID userId, Transaction.TransactionType type, String description) {
        List<Transaction> transactions = transactionRepository.findByUserIdAndTypeAndDescriptionExact(
            userId, type, description);

        if (transactions.isEmpty()) {
            return Optional.empty();
        }

        return getMostFrequentCategoryFromTransactions(transactions)
            .map(cat -> buildResponse(cat, new BigDecimal("0.95"), SuggestionSource.USER_HISTORY_EXACT));
    }

    private Optional<CategorySuggestionResponse> suggestFromSimilarHistory(
            UUID userId, Transaction.TransactionType type, String description) {
        if (description.length() < 3) {
            return Optional.empty();
        }

        String searchTerm = description.length() > 50 ? description.substring(0, 50) : description;
        List<Transaction> transactions = transactionRepository.findByUserIdAndTypeAndDescriptionContaining(
            userId, type, searchTerm);

        if (transactions.isEmpty()) {
            return Optional.empty();
        }

        return getMostFrequentCategoryFromTransactions(transactions)
            .map(cat -> buildResponse(cat, new BigDecimal("0.85"), SuggestionSource.USER_HISTORY_SIMILAR));
    }

    private Optional<CategorySuggestionResponse> suggestFromKeywords(String description, List<Category> userCategories) {
        String lower = description.toLowerCase();

        for (Map.Entry<String, List<String>> entry : KEYWORD_TO_CATEGORY_NAMES.entrySet()) {
            if (lower.contains(entry.getKey())) {
                for (String suggestedName : entry.getValue()) {
                    Optional<Category> match = userCategories.stream()
                        .filter(c -> c.getName().equalsIgnoreCase(suggestedName))
                        .findFirst();
                    if (match.isEmpty()) {
                        match = userCategories.stream()
                            .filter(c -> c.getName().toLowerCase().contains(suggestedName.toLowerCase()))
                            .findFirst();
                    }
                    if (match.isPresent()) {
                        return Optional.of(buildResponse(
                            match.get(), new BigDecimal("0.75"), SuggestionSource.KEYWORD_MATCH));
                    }
                }
            }
        }
        return Optional.empty();
    }

    private Optional<CategorySuggestionResponse> suggestMostUsedCategory(
            UUID userId, Transaction.TransactionType type) {
        List<Transaction> allByType = transactionRepository.findAllByUserIdAndTypeForReport(userId, type);
        if (allByType.isEmpty()) {
            return Optional.empty();
        }

        return getMostFrequentCategoryFromTransactions(allByType)
            .map(cat -> buildResponse(cat, new BigDecimal("0.50"), SuggestionSource.MOST_USED));
    }

    private Optional<Category> getMostFrequentCategoryFromTransactions(List<Transaction> transactions) {
        Optional<UUID> topCategoryId = transactions.stream()
            .map(Transaction::getCategory)
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(Category::getId, Collectors.counting()))
            .entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey);
        return topCategoryId.flatMap(id -> transactions.stream()
            .map(Transaction::getCategory)
            .filter(Objects::nonNull)
            .filter(c -> c.getId().equals(id))
            .findFirst());
    }

    private CategorySuggestionResponse buildResponse(Category cat, BigDecimal confidence, SuggestionSource source) {
        return CategorySuggestionResponse.builder()
            .categoryId(cat.getId())
            .categoryName(cat.getName())
            .type(cat.getType())
            .color(cat.getColor())
            .icon(cat.getIcon())
            .confidence(confidence.setScale(2, RoundingMode.HALF_UP))
            .source(source)
            .build();
    }

    private Category.CategoryType toCategoryType(Transaction.TransactionType type) {
        return type == Transaction.TransactionType.INCOME ? Category.CategoryType.INCOME : Category.CategoryType.EXPENSE;
    }

    private UUID getCurrentUserId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UUID)) {
            throw new RuntimeException("Usuário não autenticado");
        }
        return (UUID) auth.getPrincipal();
    }
}
