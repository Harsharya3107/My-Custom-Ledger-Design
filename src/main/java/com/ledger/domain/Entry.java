package com.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "entries")
@Getter
@Setter
@NoArgsConstructor
// DB-generated (BIGSERIAL) — the application never sets this, Postgres does,
// at insert time. This is the real insertion-order cursor; `id` is a random
// UUID and must never be used for ordering/pagination.
public class Entry {

    @Id
    private UUID id;

    // DB-generated (BIGSERIAL) — the application never sets this, Postgres does,
    // at insert time. This is the real insertion-order cursor; `id` is a random
    // UUID and must never be used for ordering/pagination.
    @Column(name = "sequence_number", insertable = false, updatable = false)
    private long sequenceNumber;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    // Signed, minor units (cents). Positive = credit, negative = debit.
    @Column(nullable = false)
    private long amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Entry(UUID id, UUID transactionId, UUID accountId, long amount, Instant createdAt) {
        this.id = id;
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.amount = amount;
        this.createdAt = createdAt;
    }
}
