package com.ledger.service;

import com.ledger.api.PostTransactionRequest;
import com.ledger.api.TransactionResponse;
import com.ledger.domain.Account;
import com.ledger.domain.Entry;
import com.ledger.domain.Transaction;
import com.ledger.domain.TransactionStatus;
import com.ledger.repository.AccountRepository;
import com.ledger.repository.BalanceRepository;
import com.ledger.repository.EntryRepository;
import com.ledger.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LedgerService {

    private final TransactionRepository transactionRepository;
    private final EntryRepository entryRepository;
    private final BalanceRepository balanceRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public TransactionResponse postTransaction(String idempotencyKey, PostTransactionRequest request) {
        String requestHash = hashRequest(request);

        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return replay(existing.get(), requestHash, idempotencyKey);
        }

        validate(request);

        Instant now = Instant.now();
        Transaction transaction = new Transaction(
                UUID.randomUUID(), idempotencyKey, requestHash, request.description(),
                TransactionStatus.POSTED, now);
        transactionRepository.save(transaction);

        // Write every entry and flush before adjusting any balance. adjustBalance's
        // @Modifying(clearAutomatically = true) detaches the persistence context
        // right after it runs - anything still unflushed at that point (this
        // transaction, these entries) would be silently lost rather than persisted.
        List<Entry> entries = new ArrayList<>();
        for (PostTransactionRequest.EntryLine line : request.entries()) {
            entries.add(new Entry(UUID.randomUUID(), transaction.getId(), line.accountId(), line.amount(), now));
        }
        entryRepository.saveAll(entries);
        entryRepository.flush();

        for (PostTransactionRequest.EntryLine line : request.entries()) {
            adjustBalance(line.accountId(), line.amount());
        }

        return TransactionResponse.from(transaction, entries);
    }

    public TransactionResponse getTransaction(UUID id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException("transaction not found: " + id));
        List<Entry> entries = entryRepository.findByTransactionIdOrderBySequenceNumberAsc(id);
        return TransactionResponse.from(transaction, entries);
    }

    // "Undo" a transaction by posting a new, inverted one - history is never
    // edited. Reuses postTransaction wholesale for the inverted entries, so the
    // reversal gets the same double-entry validation, atomic balance update,
    // and balance-floor check as any other write, for free.
    @Transactional
    public TransactionResponse reverseTransaction(UUID transactionId, String idempotencyKey) {
        Transaction original = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("transaction not found: " + transactionId));

        // A retry of this same reversal call must still replay cleanly even
        // though `original` is already REVERSED by the time it arrives - only
        // reject a *new* reversal key against an already-reversed transaction.
        boolean isRetryOfThisReversal = transactionRepository.findByIdempotencyKey(idempotencyKey).isPresent();
        if (!isRetryOfThisReversal && original.getStatus() == TransactionStatus.REVERSED) {
            throw new InvalidTransactionException("transaction " + transactionId + " has already been reversed");
        }

        List<Entry> originalEntries = entryRepository.findByTransactionIdOrderBySequenceNumberAsc(transactionId);
        List<PostTransactionRequest.EntryLine> reversedLines = originalEntries.stream()
                .map(e -> new PostTransactionRequest.EntryLine(e.getAccountId(), -e.getAmount()))
                .toList();
        PostTransactionRequest reversalRequest = new PostTransactionRequest(
                "reversal of " + transactionId, reversedLines);

        TransactionResponse response = postTransaction(idempotencyKey, reversalRequest);

        original.setStatus(TransactionStatus.REVERSED);
        transactionRepository.save(original);

        return response;
    }

    private TransactionResponse replay(Transaction transaction, String requestHash, String idempotencyKey) {
        if (!transaction.getRequestHash().equals(requestHash)) {
            throw new IdempotencyConflictException(
                    "Idempotency-Key '" + idempotencyKey + "' was already used with a different request body");
        }
        List<Entry> entries = entryRepository.findByTransactionIdOrderBySequenceNumberAsc(transaction.getId());
        return TransactionResponse.from(transaction, entries);
    }

    private void adjustBalance(UUID accountId, long delta) {
        int updated;
        try {
            updated = balanceRepository.adjustBalance(accountId, delta);
        } catch (DataIntegrityViolationException e) {
            throw new InsufficientFundsException("account " + accountId + " would go below its balance floor");
        }
        if (updated == 0) {
            throw new InvalidTransactionException("account " + accountId + " has no balance record");
        }
    }

    private void validate(PostTransactionRequest request) {
        List<PostTransactionRequest.EntryLine> lines = request.entries();
        if (lines == null || lines.size() < 2) {
            throw new InvalidTransactionException("a transaction requires at least two entries");
        }

        long sum = 0;
        Set<UUID> accountIds = new HashSet<>();
        for (PostTransactionRequest.EntryLine line : lines) {
            sum += line.amount();
            accountIds.add(line.accountId());
        }
        if (sum != 0) {
            throw new InvalidTransactionException("entries must sum to zero, got " + sum);
        }

        Set<UUID> knownIds = new HashSet<>();
        for (Account account : accountRepository.findAllById(accountIds)) {
            knownIds.add(account.getId());
        }
        accountIds.removeAll(knownIds);
        if (!accountIds.isEmpty()) {
            throw new InvalidTransactionException("unknown account(s): " + accountIds);
        }
    }

    private String hashRequest(PostTransactionRequest request) {
        List<PostTransactionRequest.EntryLine> lines = request.entries() == null ? List.of() : request.entries();
        StringBuilder canonical = new StringBuilder();
        canonical.append(request.description() == null ? "" : request.description()).append('|');
        for (PostTransactionRequest.EntryLine line : lines) {
            canonical.append(line.accountId()).append(':').append(line.amount()).append(',');
        }
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}