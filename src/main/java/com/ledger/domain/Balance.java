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

// Materialized cache of SUM(entries.amount) for one account, kept in sync
// transactionally with entry inserts. This is the only row in the whole schema
// that's ever contended, and the only one an UPDATE ever touches.
@Entity
@Table(name = "balances")
@Getter
@Setter
@NoArgsConstructor
public class Balance {

    @Id // to make it a primary key, as single account can have single balance
    @Column(name = "account_id")
    private UUID accountId;

    private long balance;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Balance(UUID accountId, long balance, Instant updatedAt) {
        this.accountId = accountId;
        this.balance = balance;
        this.updatedAt = updatedAt;
    }
}