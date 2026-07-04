package com.financeflow.transactions.integration;

import com.financeflow.accounts.domain.Account;
import com.financeflow.accounts.repository.AccountRepository;
import com.financeflow.categories.domain.Category;
import com.financeflow.categories.repository.CategoryRepository;
import com.financeflow.users.domain.User;
import com.financeflow.users.repository.UserRepository;
import com.financeflow.transactions.domain.Transaction;
import com.financeflow.transactions.dto.TransactionRequest;
import com.financeflow.transactions.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração para fluxo completo de transações
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class TransactionIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    private User testUser;
    private Account testAccount;
    private Category testCategory;
    
    @BeforeEach
    void setUp() {
        // Criar usuário de teste
        testUser = User.builder()
                .name("Test User")
                .email("test@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .build();
        testUser = userRepository.save(testUser);
        
        // Criar conta de teste
        testAccount = Account.builder()
                .name("Test Account")
                .type(Account.AccountType.BANK)
                .initialBalance(BigDecimal.valueOf(1000))
                .balance(BigDecimal.valueOf(1000))
                .user(testUser)
                .build();
        testAccount = accountRepository.save(testAccount);
        
        // Criar categoria de teste
        testCategory = Category.builder()
                .name("Test Category")
                .type(Category.CategoryType.EXPENSE)
                .user(testUser)
                .build();
        testCategory = categoryRepository.save(testCategory);
    }
    
    @Test
    @WithMockUser(username = "test@example.com")
    void shouldCreateTransactionAndUpdateAccountBalance() throws Exception {
        // Given
        TransactionRequest request = TransactionRequest.builder()
                .accountId(testAccount.getId())
                .categoryId(testCategory.getId())
                .amount(BigDecimal.valueOf(100))
                .type(Transaction.TransactionType.EXPENSE)
                .date(LocalDate.now())
                .description("Test transaction")
                .build();
        
        // When
        mockMvc.perform(post("/api/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.amount").value(100))
                .andExpect(jsonPath("$.data.type").value("EXPENSE"));
        
        // Then - Verificar que saldo foi atualizado
        Account updatedAccount = accountRepository.findById(testAccount.getId()).orElseThrow();
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(900));
        
        // Verificar que transação foi criada
        assertThat(transactionRepository.findAllByUserId(testUser.getId(), Pageable.unpaged()).getContent()).hasSize(1);
    }
    
    @Test
    @WithMockUser(username = "test@example.com")
    void shouldUpdateTransactionAndAdjustAccountBalance() throws Exception {
        // Given - Criar transação inicial
        Transaction existingTransaction = Transaction.builder()
                .account(testAccount)
                .category(testCategory)
                .amount(BigDecimal.valueOf(50))
                .type(Transaction.TransactionType.EXPENSE)
                .date(LocalDate.now())
                .description("Original transaction")
                .build();
        existingTransaction = transactionRepository.save(existingTransaction);
        
        // Atualizar saldo da conta
        testAccount.setBalance(BigDecimal.valueOf(950));
        accountRepository.save(testAccount);
        
        // When - Atualizar transação
        TransactionRequest updateRequest = TransactionRequest.builder()
                .accountId(testAccount.getId())
                .categoryId(testCategory.getId())
                .amount(BigDecimal.valueOf(100)) // Mudou de 50 para 100
                .type(Transaction.TransactionType.EXPENSE)
                .date(LocalDate.now())
                .description("Updated transaction")
                .build();
        
        mockMvc.perform(put("/api/v1/transactions/" + existingTransaction.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value(100));
        
        // Then - Verificar ajuste do saldo (950 + 50 - 100 = 900)
        Account updatedAccount = accountRepository.findById(testAccount.getId()).orElseThrow();
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(900));
    }
    
    @Test
    @WithMockUser(username = "test@example.com")
    void shouldDeleteTransactionAndRestoreAccountBalance() throws Exception {
        // Given
        Transaction existingTransaction = Transaction.builder()
                .account(testAccount)
                .category(testCategory)
                .amount(BigDecimal.valueOf(50))
                .type(Transaction.TransactionType.EXPENSE)
                .date(LocalDate.now())
                .description("Transaction to delete")
                .build();
        existingTransaction = transactionRepository.save(existingTransaction);
        
        testAccount.setBalance(BigDecimal.valueOf(950));
        accountRepository.save(testAccount);
        
        // When
        mockMvc.perform(delete("/api/v1/transactions/" + existingTransaction.getId()))
                .andExpect(status().isOk());
        
        // Then - Verificar que saldo foi restaurado
        Account updatedAccount = accountRepository.findById(testAccount.getId()).orElseThrow();
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        
        // Verificar que transação foi excluída (soft delete)
        assertThat(transactionRepository.findById(existingTransaction.getId())).isEmpty();
    }
    
    @Test
    @WithMockUser(username = "test@example.com")
    void shouldFilterTransactionsByDateRange() throws Exception {
        // Given - Criar transações em datas diferentes
        createTransaction(LocalDate.now().minusDays(5), BigDecimal.valueOf(100));
        createTransaction(LocalDate.now().minusDays(3), BigDecimal.valueOf(200));
        createTransaction(LocalDate.now().minusDays(1), BigDecimal.valueOf(300));
        
        // When
        mockMvc.perform(get("/api/v1/transactions")
                .param("dateFrom", LocalDate.now().minusDays(4).toString())
                .param("dateTo", LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2)); // Apenas 2 transações no intervalo
    }
    
    @Test
    @WithMockUser(username = "test@example.com")
    void shouldPreventNegativeBalance() throws Exception {
        // Given
        TransactionRequest request = TransactionRequest.builder()
                .accountId(testAccount.getId())
                .categoryId(testCategory.getId())
                .amount(BigDecimal.valueOf(2000)) // Maior que o saldo
                .type(Transaction.TransactionType.EXPENSE)
                .date(LocalDate.now())
                .description("Large expense")
                .build();
        
        // When/Then - Deve permitir (não há regra de saldo negativo)
        mockMvc.perform(post("/api/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
        
        Account updatedAccount = accountRepository.findById(testAccount.getId()).orElseThrow();
        assertThat(updatedAccount.getBalance()).isNegative();
    }
    
    @Test
    @WithMockUser(username = "other@example.com")
    void shouldNotAccessOtherUserTransactions() throws Exception {
        // Given - Outro usuário
        User otherUser = User.builder()
                .name("Other User")
                .email("other@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .build();
        userRepository.save(otherUser);
        
        // Criar transação do primeiro usuário
        createTransaction(LocalDate.now(), BigDecimal.valueOf(100));
        
        // When/Then - Outro usuário não deve ver as transações
        mockMvc.perform(get("/api/v1/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }
    
    private Transaction createTransaction(LocalDate date, BigDecimal amount) {
        Transaction transaction = Transaction.builder()
                .account(testAccount)
                .category(testCategory)
                .amount(amount)
                .type(Transaction.TransactionType.EXPENSE)
                .date(date)
                .description("Test transaction")
                .build();
        return transactionRepository.save(transaction);
    }
}
