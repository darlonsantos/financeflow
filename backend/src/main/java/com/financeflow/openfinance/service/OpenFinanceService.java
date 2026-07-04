package com.financeflow.openfinance.service;

import com.financeflow.openfinance.client.OpenFinanceProviderClient;
import com.financeflow.openfinance.client.PluggyAccountData;
import com.financeflow.openfinance.client.PluggyBillData;
import com.financeflow.openfinance.client.PluggyTransactionData;
import com.financeflow.openfinance.client.ProviderSessionResponse;
import com.financeflow.openfinance.domain.BankAccountConnection;
import com.financeflow.openfinance.domain.BankConnection;
import com.financeflow.openfinance.domain.ImportedTransaction;
import com.financeflow.openfinance.domain.SyncHistory;
import com.financeflow.openfinance.dto.BankAccountResponse;
import com.financeflow.openfinance.dto.BankConnectionResponse;
import com.financeflow.openfinance.dto.ConnectBankResponse;
import com.financeflow.openfinance.dto.CreditCardSummaryResponse;
import com.financeflow.openfinance.dto.ImportedTransactionResponse;
import com.financeflow.openfinance.dto.SyncHistoryResponse;
import com.financeflow.openfinance.repository.BankAccountConnectionRepository;
import com.financeflow.openfinance.repository.BankConnectionRepository;
import com.financeflow.openfinance.repository.ImportedTransactionRepository;
import com.financeflow.openfinance.repository.SyncHistoryRepository;
import com.financeflow.transactions.domain.Transaction;
import com.financeflow.transactions.repository.TransactionRepository;
import com.financeflow.users.domain.User;
import com.financeflow.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenFinanceService {

    private final BankConnectionRepository bankConnectionRepository;
    private final BankAccountConnectionRepository bankAccountConnectionRepository;
    private final ImportedTransactionRepository importedTransactionRepository;
    private final SyncHistoryRepository syncHistoryRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final OpenFinanceProviderClient providerClient;
    private final TokenCryptoService tokenCryptoService;

    @Transactional
    public ConnectBankResponse connect(String provider) {
        UUID userId = getCurrentUserId();
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        ProviderSessionResponse session = providerClient.createConnectionSession(userId);
        BankConnection connection = BankConnection.builder()
            .user(user)
            .provider(provider)
            .providerConnectionId(session.providerConnectionId())
            .accessToken(tokenCryptoService.encrypt(session.accessToken()))
            .refreshToken(tokenCryptoService.encrypt(session.refreshToken()))
            .expiraEm(session.expiresAt())
            .status(BankConnection.Status.PENDING)
            .build();
        connection = bankConnectionRepository.save(connection);

        log.info("Conexão Open Finance criada: connectionId={}, provider={}", connection.getId(), provider);
        return ConnectBankResponse.builder()
            .connectionId(connection.getId())
            .linkToken(session.linkToken())
            .providerConnectionId(connection.getProviderConnectionId())
            .status(connection.getStatus().name())
            .build();
    }

    @Transactional(readOnly = true)
    public List<BankConnectionResponse> listConnections() {
        UUID userId = getCurrentUserId();
        return bankConnectionRepository.findByUserIdOrderByDataCriacaoDesc(userId).stream()
            .map(this::toConnectionResponse)
            .toList();
    }

    @Transactional
    public List<BankAccountResponse> listAccounts(UUID connectionId) {
        BankConnection connection = getConnectionForCurrentUser(connectionId);
        String institutionName = providerClient.fetchInstitutionName(connection.getProviderConnectionId());
        return bankAccountConnectionRepository.findByConnectionId(connection.getId()).stream()
            .map(account -> toBankAccountResponse(account, resolveBankName(account.getBanco(), connection.getProvider(), institutionName)))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ImportedTransactionResponse> listImportedTransactions(UUID connectionId) {
        BankConnection connection = getConnectionForCurrentUser(connectionId);
        return importedTransactionRepository.findByAccountConnectionIdOrderByDataTransacaoDesc(connection.getId()).stream()
            .map(this::toImportedTransactionResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<CreditCardSummaryResponse> listCreditCardSummary(UUID connectionId) {
        BankConnection connection = getConnectionForCurrentUser(connectionId);
        ensureConnectionReady(connection);
        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = hoje.withDayOfMonth(1);
        LocalDate fimMes = hoje.withDayOfMonth(hoje.lengthOfMonth());
        return providerClient.fetchAccounts(connection.getProviderConnectionId()).stream()
            .filter(a -> "CREDIT".equalsIgnoreCase(a.type()) || "CREDIT_CARD".equalsIgnoreCase(a.subtype()))
            .map(a -> {
                List<PluggyBillData> bills = providerClient.fetchBills(a.id());
                Optional<PluggyBillData> currentBill = bills.stream()
                    .filter(b -> b.dueDate() != null && !b.dueDate().isBefore(hoje))
                    .min(Comparator.comparing(PluggyBillData::dueDate));
                if (currentBill.isEmpty()) {
                    currentBill = bills.stream()
                        .filter(b -> b.dueDate() != null)
                        .max(Comparator.comparing(PluggyBillData::dueDate));
                }

                BigDecimal fallbackByMonthTransactions = providerClient.fetchTransactions(a.id(), inicioMes, fimMes).stream()
                    .map(PluggyTransactionData::amount)
                    .filter(amount -> amount != null && amount.signum() < 0)
                    .map(BigDecimal::abs)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal totalMesCorrente = currentBill
                    .map(PluggyBillData::totalAmount)
                    .filter(v -> v != null && v.signum() >= 0)
                    .orElse(fallbackByMonthTransactions);
                totalMesCorrente = normalizeBillAmount(totalMesCorrente, a.balance());

                // Fallback final: se ainda vier zerado, usa saldo da fatura atual.
                if (totalMesCorrente == null || totalMesCorrente.signum() == 0) {
                    totalMesCorrente = a.balance() != null ? a.balance().abs() : BigDecimal.ZERO;
                }

                return CreditCardSummaryResponse.builder()
                    .providerAccountId(a.id())
                    .nomeConta(a.name())
                    .moeda(currentBill.map(PluggyBillData::currencyCode).orElse(a.currencyCode()))
                    .totalFatura(a.balance())
                    .totalFaturaMesCorrente(totalMesCorrente)
                    .pagamentoMinimo(currentBill.map(PluggyBillData::minimumPaymentAmount).orElse(a.minimumPayment()))
                    .vencimentoFatura(currentBill.map(PluggyBillData::dueDate).orElse(a.balanceDueDate()))
                    .fechamentoFatura(a.balanceCloseDate())
                    .limiteDisponivel(a.availableCreditLimit())
                    .limiteTotal(a.creditLimit())
                    .build();
            })
            .toList();
    }

    private BigDecimal normalizeBillAmount(BigDecimal amount, BigDecimal accountBalance) {
        if (amount == null) return null;
        if (accountBalance == null || accountBalance.signum() == 0) return amount;

        BigDecimal absAmount = amount.abs();
        BigDecimal absBalance = accountBalance.abs();

        // Alguns conectores retornam totalAmount em centavos; normaliza quando detectado.
        if (absAmount.compareTo(absBalance.multiply(BigDecimal.TEN)) > 0) {
            return absAmount.divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        }
        return absAmount;
    }

    @Transactional(readOnly = true)
    public List<SyncHistoryResponse> listHistory(UUID connectionId) {
        BankConnection connection = getConnectionForCurrentUser(connectionId);
        return syncHistoryRepository.findByConnectionIdOrderByDataInicioDesc(connection.getId()).stream()
            .map(this::toSyncHistoryResponse)
            .toList();
    }

    @Transactional
    public SyncHistoryResponse syncConnection(UUID connectionId, LocalDate dateFrom, LocalDate dateTo) {
        BankConnection connection = getConnectionForCurrentUser(connectionId);
        ensureConnectionReady(connection);
        return syncConnectionInternal(connection, dateFrom, dateTo);
    }

    @Transactional
    public BankConnectionResponse confirmConnection(UUID connectionId, String providerConnectionId) {
        if (providerConnectionId == null || providerConnectionId.isBlank()) {
            throw new IllegalArgumentException("providerConnectionId (itemId) é obrigatório");
        }
        BankConnection connection = getConnectionForCurrentUser(connectionId);
        connection.setProviderConnectionId(providerConnectionId.trim());
        connection.setStatus(BankConnection.Status.ACTIVE);
        connection = bankConnectionRepository.save(connection);
        return toConnectionResponse(connection);
    }

    @Transactional
    public void revokeConnection(UUID connectionId) {
        BankConnection connection = getConnectionForCurrentUser(connectionId);
        connection.setStatus(BankConnection.Status.REVOKED);
        connection.setAccessToken(null);
        connection.setRefreshToken(null);
        bankConnectionRepository.save(connection);
        log.info("Conexão Open Finance revogada: {}", connectionId);
    }

    @Transactional
    public void syncAllActiveConnections() {
        List<BankConnection> activeConnections = bankConnectionRepository.findByStatus(BankConnection.Status.ACTIVE);
        for (BankConnection connection : activeConnections) {
            try {
                syncConnectionInternal(connection, LocalDate.now().minusDays(1), LocalDate.now());
            } catch (Exception e) {
                log.error("Falha na sincronização automática da conexão {}", connection.getId(), e);
            }
        }
    }

    private SyncHistoryResponse syncConnectionInternal(BankConnection connection, LocalDate dateFrom, LocalDate dateTo) {
        LocalDate to = dateTo == null ? LocalDate.now() : dateTo;
        LocalDate from = resolveSyncStartDate(connection, dateFrom, to);
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("Data final não pode ser anterior à data inicial");
        }

        SyncHistory history = SyncHistory.builder()
            .connection(connection)
            .status(SyncHistory.Status.SUCCESS)
            .build();
        history = syncHistoryRepository.save(history);

        try {
            int importedCount = 0;
            int conflictCount = 0;
            String institutionName = providerClient.fetchInstitutionName(connection.getProviderConnectionId());
            List<PluggyAccountData> providerAccounts = providerClient.fetchAccounts(connection.getProviderConnectionId());
            for (PluggyAccountData providerAccount : providerAccounts) {
                BankAccountConnection account = upsertBankAccount(connection, providerAccount, institutionName);
                List<PluggyTransactionData> providerTransactions = providerClient.fetchTransactions(
                    providerAccount.id(),
                    from,
                    to
                );
                for (PluggyTransactionData item : providerTransactions) {
                    Optional<ImportedTransaction> existing = importedTransactionRepository
                        .findByAccountIdAndProviderTransactionId(account.getId(), item.id());
                    if (existing.isPresent()) {
                        continue;
                    }

                    String hash = buildUniqueHash(connection.getId(), item.id(), item.amount(), item.date());
                    if (importedTransactionRepository.existsByHashUnico(hash)) {
                        continue;
                    }

                    ImportedTransaction imported = ImportedTransaction.builder()
                        .account(account)
                        .providerTransactionId(item.id())
                        .descricao(item.description())
                        .valor(item.amount())
                        .dataTransacao(item.date())
                        .categoriaSugerida(item.category() != null ? item.category() : (item.amount().signum() < 0 ? "DESPESA" : "RECEITA"))
                        .hashUnico(hash)
                        .statusConciliacao(ImportedTransaction.StatusConciliacao.PENDENTE)
                        .build();

                    Optional<Transaction> matched = matchTransaction(connection.getUser().getId(), imported);
                    if (matched.isPresent()) {
                        imported.setTransaction(matched.get());
                        imported.setStatusConciliacao(ImportedTransaction.StatusConciliacao.CONCILIADO);
                    } else if (isConflict(imported)) {
                        imported.setStatusConciliacao(ImportedTransaction.StatusConciliacao.CONFLITO);
                        conflictCount++;
                    }

                    importedTransactionRepository.save(imported);
                    importedCount++;
                }
            }

            history.setDataFim(LocalDateTime.now());
            history.setTotalImportado(importedCount);
            history.setConflitos(conflictCount);
            history.setStatus(SyncHistory.Status.SUCCESS);
            history = syncHistoryRepository.save(history);
            log.info("Sincronização Open Finance concluída: connectionId={}, importadas={}, conflitos={}",
                connection.getId(), importedCount, conflictCount);
            return toSyncHistoryResponse(history);
        } catch (Exception e) {
            history.setDataFim(LocalDateTime.now());
            history.setStatus(SyncHistory.Status.ERROR);
            history.setMensagemErro(e.getMessage());
            history = syncHistoryRepository.save(history);
            throw e;
        }
    }

    private void ensureConnectionReady(BankConnection connection) {
        if (connection.getStatus() == BankConnection.Status.PENDING) {
            throw new IllegalStateException("Conexão pendente: confirme o itemId retornado pela Pluggy para ativar");
        }
    }

    private LocalDate resolveSyncStartDate(BankConnection connection, LocalDate requestedFrom, LocalDate to) {
        if (requestedFrom != null) {
            return requestedFrom;
        }
        // Sincronização manual sem período informado: busca histórico amplo.
        return to.minusMonths(12);
    }

    private Optional<Transaction> matchTransaction(UUID userId, ImportedTransaction imported) {
        Transaction.TransactionType type = imported.getValor().signum() >= 0
            ? Transaction.TransactionType.INCOME
            : Transaction.TransactionType.EXPENSE;
        List<Transaction> candidates = transactionRepository.findByUserIdAndTypeAndDescriptionContaining(
            userId, type, safeSubstring(imported.getDescricao(), 15)
        );
        BigDecimal absoluteAmount = imported.getValor().abs();
        return candidates.stream()
            .filter(t -> t.getAmount().compareTo(absoluteAmount) == 0)
            .filter(t -> !t.getDate().isBefore(imported.getDataTransacao().minusDays(2)))
            .filter(t -> !t.getDate().isAfter(imported.getDataTransacao().plusDays(2)))
            .findFirst();
    }

    private boolean isConflict(ImportedTransaction imported) {
        return imported.getDescricao() != null && imported.getDescricao().toLowerCase().contains("duplicado");
    }

    private String safeSubstring(String text, int maxSize) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String value = text.trim();
        if (value.length() <= maxSize) {
            return value;
        }
        return value.substring(0, maxSize);
    }

    private BankAccountConnection upsertBankAccount(BankConnection connection, PluggyAccountData providerAccount, String institutionName) {
        String bankName = resolveBankName(null, connection.getProvider(), institutionName);
        return bankAccountConnectionRepository
            .findByConnectionIdAndProviderAccountId(connection.getId(), providerAccount.id())
            .map(existing -> {
                existing.setNome(providerAccount.name());
                existing.setSaldoAtual(providerAccount.balance());
                existing.setTipoConta(providerAccount.subtype());
                existing.setBanco(resolveBankName(existing.getBanco(), connection.getProvider(), institutionName));
                return bankAccountConnectionRepository.save(existing);
            })
            .orElseGet(() -> bankAccountConnectionRepository.save(
                BankAccountConnection.builder()
                    .user(connection.getUser())
                    .connection(connection)
                    .providerAccountId(providerAccount.id())
                    .nome(providerAccount.name())
                    .banco(bankName)
                    .saldoAtual(providerAccount.balance())
                    .tipoConta(providerAccount.subtype())
                    .build()
            ));
    }

    private String resolveBankName(String currentBankName, String provider, String institutionName) {
        if (StringUtils.hasText(institutionName)) {
            return institutionName;
        }
        if (StringUtils.hasText(currentBankName) && !"PLUGGY".equalsIgnoreCase(currentBankName.trim())) {
            return currentBankName;
        }
        return provider == null ? "BANCO" : provider.toUpperCase();
    }

    private String buildUniqueHash(UUID connectionId, String providerTransactionId, BigDecimal amount, LocalDate date) {
        try {
            String raw = connectionId + "|" + providerTransactionId + "|" + amount + "|" + date;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao gerar hash de transação importada", e);
        }
    }

    private BankConnection getConnectionForCurrentUser(UUID connectionId) {
        UUID userId = getCurrentUserId();
        return bankConnectionRepository.findByIdAndUserId(connectionId, userId)
            .orElseThrow(() -> new RuntimeException("Conexão bancária não encontrada"));
    }

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UUID)) {
            throw new RuntimeException("Usuário não autenticado");
        }
        return (UUID) authentication.getPrincipal();
    }

    private BankConnectionResponse toConnectionResponse(BankConnection connection) {
        return BankConnectionResponse.builder()
            .id(connection.getId())
            .provider(connection.getProvider())
            .providerConnectionId(connection.getProviderConnectionId())
            .status(connection.getStatus().name())
            .expiraEm(connection.getExpiraEm())
            .dataCriacao(connection.getDataCriacao())
            .build();
    }

    private BankAccountResponse toBankAccountResponse(BankAccountConnection account, String bankName) {
        return BankAccountResponse.builder()
            .id(account.getId())
            .providerAccountId(account.getProviderAccountId())
            .nome(account.getNome())
            .banco(bankName)
            .saldoAtual(account.getSaldoAtual())
            .tipoConta(account.getTipoConta())
            .build();
    }

    private ImportedTransactionResponse toImportedTransactionResponse(ImportedTransaction item) {
        return ImportedTransactionResponse.builder()
            .id(item.getId())
            .providerTransactionId(item.getProviderTransactionId())
            .descricao(item.getDescricao())
            .valor(item.getValor())
            .dataTransacao(item.getDataTransacao())
            .categoriaSugerida(item.getCategoriaSugerida())
            .statusConciliacao(item.getStatusConciliacao().name())
            .transactionId(item.getTransaction() != null ? item.getTransaction().getId() : null)
            .build();
    }

    private SyncHistoryResponse toSyncHistoryResponse(SyncHistory item) {
        return SyncHistoryResponse.builder()
            .id(item.getId())
            .dataInicio(item.getDataInicio())
            .dataFim(item.getDataFim())
            .totalImportado(item.getTotalImportado())
            .conflitos(item.getConflitos())
            .status(item.getStatus().name())
            .mensagemErro(item.getMensagemErro())
            .build();
    }
}
