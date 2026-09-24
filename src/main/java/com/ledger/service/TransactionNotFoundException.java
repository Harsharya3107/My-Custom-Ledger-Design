package com.ledger.service;

// 404 — no transaction exists with this id.
public class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException(String message) {
        super(message);
    }
}
