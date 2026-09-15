package com.ledger.service;

// 409 — same Idempotency-Key reused with a different request body.
// A client bug, not a valid retry.
public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException(String message) {
        super(message);
    }
}