package com.ledger.repository;

import com.ledger.domain.Balance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface BalanceRepository extends JpaRepository<Balance, UUID> {

    // The atomic single-statement update from the concurrency design: Postgres
    // takes a row lock on this specific balances row for the statement's
    // duration and computes balance + delta off the current on-disk value.
    // A second concurrent call simply blocks, then re-reads the fresh value —
    // no SELECT-then-check-then-write race window exists here at all.
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Balance b SET b.balance + :delta, b.updatedAt = CURRENT_TIMESTAMP " + "WHERE b.accountId = :accountId")
    int adjustBalance(@Param("accountId") UUID accountId, @Param("delta") long delta);

}
