package com.ledger.api;

import com.ledger.domain.AccountType;

public record CreateAccountRequest(String name, AccountType type, String currency) {}
