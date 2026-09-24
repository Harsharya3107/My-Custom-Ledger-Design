package com.ledger.service;

// 404 — no account exists with this id.
public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String message) {
        super(message);
    }
}
