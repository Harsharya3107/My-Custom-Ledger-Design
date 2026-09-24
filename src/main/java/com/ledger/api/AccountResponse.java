package com.ledger.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ledger.domain.Account;
import com.ledger.domain.AccountType;

import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String name,
        AccountType type,
        String currency,
        @JsonProperty("created_at") Instant createdAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(), account.getName(), account.getType(),
                account.getCurrency(), account.getCreatedAt());
    }
}
