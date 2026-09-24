package com.ledger.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ledger.domain.Entry;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// The statement response for GET /v1/accounts/{id}/entries. nextCursor is the
// sequence_number of the last entry in this page - pass it back as `after` to
// fetch the next one. null means there's nothing left to page through.
public record StatementPage(List<Line> entries, @JsonProperty("next_cursor") Long nextCursor) {

    public record Line(
            @JsonProperty("sequence_number") long sequenceNumber,
            UUID id,
            @JsonProperty("transaction_id") UUID transactionId,
            long amount,
            @JsonProperty("created_at") Instant createdAt
    ) {
        public static Line from(Entry entry) {
            return new Line(entry.getSequenceNumber(), entry.getId(), entry.getTransactionId(),
                    entry.getAmount(), entry.getCreatedAt());
        }
    }

    public static StatementPage from(List<Entry> page) {
        List<Line> lines = page.stream().map(Line::from).toList();
        Long nextCursor = page.isEmpty() ? null : page.get(page.size() - 1).getSequenceNumber();
        return new StatementPage(lines, nextCursor);
    }
}
