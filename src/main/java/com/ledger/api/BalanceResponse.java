package com.ledger.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ledger.domain.Balance;

import java.time.Instant;
import java.util.UUID;

public record BalanceResponse(
        @JsonProperty("account_id") UUID accountId,
        long balance,
        @JsonProperty("updated_at") Instant updatedAt
) {
    public static BalanceResponse from(Balance balance) {
        return new BalanceResponse(balance.getAccountId(), balance.getBalance(), balance.getUpdatedAt());
    }
}
