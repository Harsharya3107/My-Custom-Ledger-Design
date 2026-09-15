package com.ledger.service;

// 400 — the request shape itself is wrong: entries don't sum to zero,
// fewer than 2 entries, an unknown account referenced.
public class InvalidTransactionException extends RuntimeException {
    public InvalidTransactionException(String message) {
        super(message);
    }
}