package com.financeflow.transfers.service;

import com.financeflow.accounts.calculator.AccountBalanceCalculator;
import com.financeflow.accounts.domain.Account;
import com.financeflow.accounts.repository.AccountRepository;
import com.financeflow.transfers.domain.Transfer;
import com.financeflow.transfers.dto.TransferListItemResponse;
import com.financeflow.transfers.dto.TransferRequest;
import com.financeflow.transfers.dto.TransferResponse;
import com.financeflow.transfers.repository.TransferRepository;
import com.financeflow.users.domain.User;
import com.financeflow.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final UserRepository userRepository;
    private final AccountBalanceCalculator balanceCalculator;

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UUID)) {
            throw new RuntimeException("Usuário não autenticado");
        }
        return (UUID) auth.getPrincipal();
    }

    /**
     * Cria uma transferência entre contas atualizando apenas os saldos da conta de origem
     * e da conta de destino. Não cria transações de despesa/receita.
     */
    @Transactional
    public TransferResponse create(TransferRequest request) {
        UUID userId = getCurrentUserId();

        if (request.getOriginAccountId().equals(request.getDestinationAccountId())) {
            throw new IllegalArgumentException("Conta de origem e destino devem ser diferentes.");
        }

        Account originAccount = accountRepository.findByIdAccessibleByUser(request.getOriginAccountId(), userId)
            .orElseThrow(() -> new RuntimeException("Conta de origem não encontrada"));
        Account destinationAccount = accountRepository.findByIdAccessibleByUser(request.getDestinationAccountId(), userId)
            .orElseThrow(() -> new RuntimeException("Conta de destino não encontrada"));

        BigDecimal amount = request.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor da transferência deve ser maior que zero.");
        }

        String description = request.getDescription() != null && !request.getDescription().isBlank()
            ? request.getDescription().trim()
            : "Transferência entre Contas";

        // Atualiza apenas os saldos: debita origem e credita destino
        balanceCalculator.updateBalance(originAccount, amount.negate());
        balanceCalculator.updateBalance(destinationAccount, amount);
        accountRepository.save(originAccount);
        accountRepository.save(destinationAccount);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        LocalDate transferDate = request.getTransferDate() != null
            ? request.getTransferDate()
            : LocalDate.now();

        Transfer transfer = Transfer.builder()
            .user(user)
            .originAccount(originAccount)
            .destinationAccount(destinationAccount)
            .amount(amount)
            .transferDate(transferDate)
            .description(description)
            .build();
        transfer = transferRepository.save(transfer);

        log.info("Transfer created: origin={}, destination={}, amount={}",
            request.getOriginAccountId(), request.getDestinationAccountId(), amount);

        return toResponse(transfer);
    }

    @Transactional(readOnly = true)
    public List<TransferListItemResponse> list() {
        UUID userId = getCurrentUserId();
        List<Transfer> transfers = transferRepository.findAllByUserIdOrderByTransferDateDesc(userId);
        return transfers.stream()
            .map(this::toListItemResponse)
            .toList();
    }

    /**
     * Exclui uma transferência revertendo apenas os saldos da conta de origem e de destino.
     */
    @Transactional
    public void deleteTransfer(UUID transferId) {
        UUID userId = getCurrentUserId();
        Transfer transfer = transferRepository.findByIdAndUserId(transferId, userId)
            .orElseThrow(() -> new RuntimeException("Transferência não encontrada"));

        BigDecimal amount = transfer.getAmount();
        Account origin = transfer.getOriginAccount();
        Account destination = transfer.getDestinationAccount();

        // Reverte os saldos: credita origem e debita destino
        balanceCalculator.updateBalance(origin, amount);
        balanceCalculator.updateBalance(destination, amount.negate());
        accountRepository.save(origin);
        accountRepository.save(destination);

        transferRepository.delete(transfer);
        log.info("Transfer deleted: id={}", transferId);
    }

    private TransferResponse toResponse(Transfer t) {
        return TransferResponse.builder()
            .id(t.getId())
            .originAccountName(t.getOriginAccount().getName())
            .destinationAccountName(t.getDestinationAccount().getName())
            .transferDate(t.getTransferDate())
            .amount(t.getAmount())
            .description(t.getDescription())
            .build();
    }

    private TransferListItemResponse toListItemResponse(Transfer t) {
        return TransferListItemResponse.builder()
            .id(t.getId())
            .originAccountName(t.getOriginAccount().getName())
            .destinationAccountName(t.getDestinationAccount().getName())
            .transferDate(t.getTransferDate())
            .amount(t.getAmount())
            .description(t.getDescription() != null && !t.getDescription().isBlank() ? t.getDescription() : "Transferência entre Contas")
            .build();
    }
}
