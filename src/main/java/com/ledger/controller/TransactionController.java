package com.ledger.controller;

import com.ledger.api.PostTransactionRequest;
import com.ledger.api.TransactionResponse;
import com.ledger.service.LedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final LedgerService ledgerService;

    @PostMapping
    public ResponseEntity<TransactionResponse> postTransaction(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody PostTransactionRequest request) {

        TransactionResponse response = ledgerService.postTransaction(idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public TransactionResponse getTransaction(@PathVariable UUID id) {
        return ledgerService.getTransaction(id);
    }

    @PostMapping("/{id}/reverse")
    public ResponseEntity<TransactionResponse> reverseTransaction(
            @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        TransactionResponse response = ledgerService.reverseTransaction(id, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
