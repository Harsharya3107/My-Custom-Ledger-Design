package com.ledger.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public class PostTransactionRequest(
        String description,
        List<EntryLine> entries
) {
    public record EntryLine(
            @JsonProperty("account_id")UUID accountId,
            long amount
    ) {}
}
