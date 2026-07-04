package com.financeflow.accounts.service;

import com.financeflow.accounts.domain.Account;
import com.financeflow.accounts.dto.AccountRequest;
import com.financeflow.accounts.dto.AccountResponse;
import com.financeflow.accounts.exception.AccountNotFoundException;
import com.financeflow.accounts.repository.AccountRepository;
import com.financeflow.users.domain.User;
import com.financeflow.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AccountService accountService;

    private UUID testUserId;
    private User testUser;
    private Account testAccount;
    private AccountRequest accountRequest;

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
                .type(Account.AccountType.BANK)
                .balance(BigDecimal.valueOf(1000.00))
                .initialBalance(BigDecimal.valueOf(1000.00))
                .color("#3B82F6")
                .build();

        accountRequest = new AccountRequest();
        accountRequest.setName("New Account");
        accountRequest.setType(Account.AccountType.CASH);
        accountRequest.setInitialBalance(BigDecimal.valueOf(500.00));
        accountRequest.setColor("#10B981");

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(testUserId);
    }

    @Test
    void testFindAll_Success() {
        // Arrange
        List<Account> accounts = Arrays.asList(testAccount);
        when(accountRepository.findAllByUserId(testUserId)).thenReturn(accounts);

        // Act
        List<AccountResponse> result = accountService.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Account", result.get(0).getName());
        verify(accountRepository).findAllByUserId(testUserId);
    }

    @Test
    void testFindById_Success() {
        // Arrange
        UUID accountId = testAccount.getId();
        when(accountRepository.findByIdAndUserId(accountId, testUserId))
                .thenReturn(Optional.of(testAccount));

        // Act
        AccountResponse result = accountService.findById(accountId);

        // Assert
        assertNotNull(result);
        assertEquals("Test Account", result.getName());
        assertEquals(BigDecimal.valueOf(1000.00), result.getBalance());
        verify(accountRepository).findByIdAndUserId(accountId, testUserId);
    }

    @Test
    void testFindById_NotFound() {
        // Arrange
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findByIdAndUserId(accountId, testUserId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AccountNotFoundException.class, () -> {
            accountService.findById(accountId);
        });
        verify(accountRepository).findByIdAndUserId(accountId, testUserId);
    }

    @Test
    void testCreate_Success() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // Act
        AccountResponse result = accountService.create(accountRequest);

        // Assert
        assertNotNull(result);
        verify(userRepository).findById(testUserId);
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void testUpdate_Success() {
        // Arrange
        UUID accountId = testAccount.getId();
        when(accountRepository.findByIdAndUserId(accountId, testUserId))
                .thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // Act
        AccountResponse result = accountService.update(accountId, accountRequest);

        // Assert
        assertNotNull(result);
        verify(accountRepository).findByIdAndUserId(accountId, testUserId);
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void testUpdate_RecalculateBalanceWhenInitialBalanceChanges() {
        // Arrange
        UUID accountId = testAccount.getId();
        AccountRequest updateRequest = new AccountRequest();
        updateRequest.setName("Updated Account");
        updateRequest.setType(Account.AccountType.BANK);
        updateRequest.setInitialBalance(BigDecimal.valueOf(2000.00)); // Changed from 1000

        when(accountRepository.findByIdAndUserId(accountId, testUserId))
                .thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account saved = invocation.getArgument(0);
            assertEquals(BigDecimal.valueOf(2000.00), saved.getInitialBalance());
            return saved;
        });

        // Act
        accountService.update(accountId, updateRequest);

        // Assert
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void testDelete_Success() {
        // Arrange
        UUID accountId = testAccount.getId();
        when(accountRepository.findByIdAndUserId(accountId, testUserId))
                .thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // Act
        accountService.delete(accountId);

        // Assert
        verify(accountRepository).findByIdAndUserId(accountId, testUserId);
        verify(accountRepository).save(any(Account.class));
        assertNotNull(testAccount.getDeletedAt());
    }

    @Test
    void testDelete_NotFound() {
        // Arrange
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findByIdAndUserId(accountId, testUserId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AccountNotFoundException.class, () -> {
            accountService.delete(accountId);
        });
        verify(accountRepository).findByIdAndUserId(accountId, testUserId);
        verify(accountRepository, never()).save(any(Account.class));
    }
}
