package com.financeflow.transactions.dto;

import com.financeflow.categories.domain.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorySuggestionResponse {

    private UUID categoryId;
    private String categoryName;
    private Category.CategoryType type;
    private String color;
    private String icon;
    private BigDecimal confidence;
    private SuggestionSource source;

    public enum SuggestionSource {
        /** Baseado em transação anterior com mesma descrição */
        USER_HISTORY_EXACT,
        /** Baseado em transação anterior com descrição similar */
        USER_HISTORY_SIMILAR,
        /** Baseado em padrão recorrente (mesma descrição em vários meses) */
        RECURRING_PATTERN,
        /** Baseado em palavras-chave da descrição */
        KEYWORD_MATCH,
        /** Baseado na categoria mais usada do usuário para este tipo */
        MOST_USED
    }
}
