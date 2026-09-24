package com.ledger.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ledger.domain.Entry;
import com.ledger.domain.Transaction;
import com.ledger.domain.TransactionStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        TransactionStatus status,
        @JsonProperty("created_at")Instant createdAt,
        List<EntryResponse> entries
) {
    public record EntryResponse(
            UUID id,
            @JsonProperty("account_id") UUID accountId,
            long amount
    ) {}

    // Maps our internal domain objects to the external API shape. This boundary
    // is the whole reason TransactionResponse exists as a separate type at all.
    public static TransactionResponse from(Transaction transaction, List<Entry> entries) {
        List<EntryResponse> entryResponse = entries.stream()
                .map(e -> new EntryResponse(e.getId(), e.getAccountId(), e.getAmount()))
                .toList();
        return new TransactionResponse(
                transaction.getId(),
                transaction.getStatus(),
                transaction.getCreatedAt(),
                entryResponse
        );
    }
}
