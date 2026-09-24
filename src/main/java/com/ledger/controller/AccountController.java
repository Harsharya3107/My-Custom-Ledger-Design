package com.ledger.controller;

import com.ledger.api.AccountResponse;
import com.ledger.api.BalanceResponse;
import com.ledger.api.CreateAccountRequest;
import com.ledger.api.StatementPage;
import com.ledger.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@RequestBody CreateAccountRequest request) {
        var account = accountService.createAccount(request.name(), request.type(), request.currency());
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.from(account));
    }

    @GetMapping("/{id}")
    public AccountResponse getAccount(@PathVariable UUID id) {
        return AccountResponse.from(accountService.getAccount(id));
    }

    @GetMapping("/{id}/balance")
    public BalanceResponse getBalance(@PathVariable UUID id) {
        return BalanceResponse.from(accountService.getBalance(id));
    }

    @GetMapping("/{id}/entries")
    public StatementPage getStatement(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") long after,
            @RequestParam(defaultValue = "50") int limit) {
        return StatementPage.from(accountService.getStatement(id, after, limit));
    }
}
