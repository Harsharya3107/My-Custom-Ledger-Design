package com.ledger.repository;

import com.ledger.domain.Entry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EntryRepository extends JpaRepository<Entry, UUID> {

    // Cursor-based statement pagination (Step 3): "give me the entries for this
    // account with a sequence_number after my last-seen cursor." Called with
    // afterSequenceNumber = 0 for the first page.
    List<Entry> findByAccountIdAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(
            UUID accountId, long afterSequenceNumber, Pageable limit
    );

    // Backs the idempotent-replay path in LedgerService: rebuild the response
    // for an already-posted transaction without touching accounts.balance again.
    List<Entry> findByTransactionIdOrderBySequenceNumberAsc(UUID transactionId);
}
