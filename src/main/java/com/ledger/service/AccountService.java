package com.ledger.service;

import com.ledger.domain.Account;
import com.ledger.domain.AccountType;
import com.ledger.domain.Balance;
import com.ledger.domain.Entry;
import com.ledger.repository.AccountRepository;
import com.ledger.repository.BalanceRepository;
import com.ledger.repository.EntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final BalanceRepository balanceRepository;
    private final EntryRepository entryRepository;

    // Every account gets a balances row at creation time, seeded to zero.
    // LedgerService.adjustBalance relies on this row already existing - it
    // never creates one on the fly, since a missing row is meant to be a loud
    // bug (an account nothing ever provisioned properly), not a silent no-op.
    @Transactional
    public Account createAccount(String name, AccountType type, String currency) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        Account account = new Account(id, name, type, currency, now);
        accountRepository.save(account);
        balanceRepository.save(new Balance(id, 0L, now));
        return account;
    }

    public Account getAccount(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("account not found: " + id));
    }

    public Balance getBalance(UUID id) {
        return balanceRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("account not found: " + id));
    }

    // Cursor pagination, not offset: `after` is the last-seen sequence_number,
    // 0 for the first page. Confirms the account exists first so a bad id
    // reports 404 instead of a silently empty page.
    public List<Entry> getStatement(UUID accountId, long after, int limit) {
        getAccount(accountId);
        return entryRepository.findByAccountIdAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(
                accountId, after, PageRequest.of(0, limit));
    }
}
