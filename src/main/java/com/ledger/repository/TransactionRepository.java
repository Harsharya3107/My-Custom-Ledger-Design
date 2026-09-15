package com.ledger.repository;

import com.ledger.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    // Backs the idempotency check from Step 3: on retry, look the request up by
    // this key instead of reprocessing it.
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
}
