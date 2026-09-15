package com.ledger.service;

// 422 — the request was well-formed, but posting it would take a USER
// account negative. Only detectable inside the DB transaction (Step 5),
// never at validation time.
public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}