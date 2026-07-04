package com.financeflow.transactions.service;

import com.financeflow.accounts.domain.Account;
import com.financeflow.accounts.repository.AccountRepository;
import com.financeflow.categories.domain.Category;
import com.financeflow.categories.repository.CategoryRepository;
import com.financeflow.transactions.domain.Transaction;
import com.financeflow.transactions.dto.TransactionRequest;
import com.financeflow.transactions.dto.TransactionResponse;
import com.financeflow.transactions.exception.TransactionNotFoundException;
import com.financeflow.transactions.repository.TransactionRepository;
import com.financeflow.users.domain.User;
import com.financeflow.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TransactionService transactionService;

    private UUID testUserId;
    private User testUser;
    private Account testAccount;
    private Category testCategory;
    private Transaction testTransaction;
    private TransactionRequest transactionRequest;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = User.builder()
                .id(testUserId)
                .name("Test User")
                .email("test@example.com")
                .build();

        testAccount = Account.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .name("Test Account")
                .balance(BigDecimal.valueOf(1000.00))
                .initialBalance(BigDecimal.valueOf(1000.00))
                .build();

        testCategory = Category.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .name("Test Category")
                .type(Category.CategoryType.EXPENSE)
                .build();

        testTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .account(testAccount)
                .category(testCategory)
                .amount(BigDecimal.valueOf(100.00))
                .type(Transaction.TransactionType.EXPENSE)
                .date(LocalDate.now())
                .description("Test Transaction")
                .build();

        transactionRequest = new TransactionRequest();
        transactionRequest.setAccountId(testAccount.getId());
        transactionRequest.setCategoryId(testCategory.getId());
        transactionRequest.setAmount(BigDecimal.valueOf(50.00));
        transactionRequest.setType(Transaction.TransactionType.EXPENSE);
        transactionRequest.setDate(LocalDate.now());
        transactionRequest.setDescription("New Transaction");

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(testUserId);
    }

    @Test
    void testCreate_IncomeTransaction_UpdatesAccountBalance() {
        // Arrange
        transactionRequest.setType(Transaction.TransactionType.INCOME);
        Category incomeCategory = Category.builder()
                .id(UUID.randomUUID())
                .type(Category.CategoryType.INCOME)
                .build();
        transactionRequest.setCategoryId(incomeCategory.getId());

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(accountRepository.findByIdAndUserId(testAccount.getId(), testUserId))
                .thenReturn(Optional.of(testAccount));
        when(categoryRepository.findByIdAndUserId(incomeCategory.getId(), testUserId))
                .thenReturn(Optional.of(incomeCategory));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // Act
        TransactionResponse result = transactionService.create(transactionRequest);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account savedAccount = accountCaptor.getValue();
        assertEquals(BigDecimal.valueOf(1050.00), savedAccount.getBalance()); // 1000 + 50
    }

    @Test
    void testCreate_ExpenseTransaction_UpdatesAccountBalance() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(accountRepository.findByIdAndUserId(testAccount.getId(), testUserId))
                .thenReturn(Optional.of(testAccount));
        when(categoryRepository.findByIdAndUserId(testCategory.getId(), testUserId))
                .thenReturn(Optional.of(testCategory));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // Act
        TransactionResponse result = transactionService.create(transactionRequest);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account savedAccount = accountCaptor.getValue();
        assertEquals(BigDecimal.valueOf(950.00), savedAccount.getBalance()); // 1000 - 50
    }

    @Test
    void testCreate_InvalidCategoryType() {
        // Arrange
        Category incomeCategory = Category.builder()
                .id(UUID.randomUUID())
                .type(Category.CategoryType.INCOME) // Category is INCOME but transaction is EXPENSE
                .build();
        transactionRequest.setCategoryId(incomeCategory.getId());

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(accountRepository.findByIdAndUserId(testAccount.getId(), testUserId))
                .thenReturn(Optional.of(testAccount));
        when(categoryRepository.findByIdAndUserId(incomeCategory.getId(), testUserId))
                .thenReturn(Optional.of(incomeCategory));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            transactionService.create(transactionRequest);
        });

        assertEquals("Tipo da transação não corresponde ao tipo da categoria", exception.getMessage());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void testUpdate_ChangesAccountBalance() {
        // Arrange
        BigDecimal oldAmount = BigDecimal.valueOf(100.00);
        BigDecimal newAmount = BigDecimal.valueOf(200.00);
        Transaction oldTransaction = Transaction.builder()
                .id(testTransaction.getId())
                .account(testAccount)
                .amount(oldAmount)
                .type(Transaction.TransactionType.EXPENSE)
                .build();

        transactionRequest.setAmount(newAmount);

        when(transactionRepository.findByIdAndUserId(testTransaction.getId(), testUserId))
                .thenReturn(Optional.of(oldTransaction));
        when(accountRepository.findByIdAndUserId(testAccount.getId(), testUserId))
                .thenReturn(Optional.of(testAccount));
        when(categoryRepository.findByIdAndUserId(testCategory.getId(), testUserId))
                .thenReturn(Optional.of(testCategory));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // Act
        TransactionResponse result = transactionService.update(testTransaction.getId(), transactionRequest);

        // Assert
        assertNotNull(result);
        // Should save account twice: once to revert old transaction, once to apply new
        verify(accountRepository, times(2)).save(any(Account.class));
    }

    @Test
    void testDelete_RevertsAccountBalance() {
        // Arrange
        when(transactionRepository.findByIdAndUserId(testTransaction.getId(), testUserId))
                .thenReturn(Optional.of(testTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // Act
        transactionService.delete(testTransaction.getId());

        // Assert
        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account savedAccount = accountCaptor.getValue();
        // Balance should be reverted: 1000 + 100 = 1100 (expense was removed)
        assertEquals(BigDecimal.valueOf(1100.00), savedAccount.getBalance());
        verify(transactionRepository).save(any(Transaction.class));
        assertNotNull(testTransaction.getDeletedAt());
    }

    @Test
    void testFindById_Success() {
        // Arrange
        UUID transactionId = testTransaction.getId();
        when(transactionRepository.findByIdAndUserId(transactionId, testUserId))
                .thenReturn(Optional.of(testTransaction));

        // Act
        TransactionResponse result = transactionService.findById(transactionId);

        // Assert
        assertNotNull(result);
        assertEquals(transactionId, result.getId());
        verify(transactionRepository).findByIdAndUserId(transactionId, testUserId);
    }

    @Test
    void testFindById_NotFound() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        when(transactionRepository.findByIdAndUserId(transactionId, testUserId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(TransactionNotFoundException.class, () -> {
            transactionService.findById(transactionId);
        });
    }

    @Test
    void testFindByAccount_AccountNotFound() {
        // Arrange
        UUID accountId = UUID.randomUUID();
        when(accountRepository.existsByIdAndUserId(accountId, testUserId))
                .thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            transactionService.findByAccount(accountId, PageRequest.of(0, 10));
        });

        assertEquals("Conta não encontrada", exception.getMessage());
    }

    @Test
    void testRecalculateAccountBalance() {
        // Arrange
        Transaction incomeTransaction = Transaction.builder()
                .amount(BigDecimal.valueOf(200.00))
                .type(Transaction.TransactionType.INCOME)
                .build();
        Transaction expenseTransaction = Transaction.builder()
                .amount(BigDecimal.valueOf(50.00))
                .type(Transaction.TransactionType.EXPENSE)
                .build();

        when(accountRepository.findByIdAndUserId(testAccount.getId(), testUserId))
                .thenReturn(Optional.of(testAccount));
        when(transactionRepository.findAllByAccountId(testAccount.getId()))
                .thenReturn(Arrays.asList(incomeTransaction, expenseTransaction));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // Act
        transactionService.recalculateAccountBalance(testAccount.getId());

        // Assert
        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account savedAccount = accountCaptor.getValue();
        // Initial balance (1000) + income (200) - expense (50) = 1150
        assertEquals(BigDecimal.valueOf(1150.00), savedAccount.getBalance());
    }
}
