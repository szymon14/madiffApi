package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.example.data.Account;
import org.example.data.ExchangeRequest;
import org.example.data.dto.AccountDTO;
import org.example.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService service;

    @Operation(summary = "Create a new account", description = "Creates a new account with an initial balance.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload")
    })
    @PostMapping
    public ResponseEntity<AccountDTO> createAccount(
            @RequestBody @Parameter(description = "Account details to create a new account, amount is rounded up to 2 decimal places") Account account) {
        return ResponseEntity.ok(service.createAccount(account));
    }

    @Operation(summary = "Get account details", description = "Fetches account details by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account found successfully"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Account> getAccount(
            @PathVariable @Parameter(description = "ID of the account to fetch") Long id) {
        return ResponseEntity.ok(service.getAccount(id));
    }

    @Operation(summary = "Exchange currency", description = "Exchanges currency between PLN and USD for a specific account.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Currency exchanged successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid currency or insufficient funds"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    @PostMapping("/exchange")
    public ResponseEntity<Account> exchangeCurrency(
            @RequestBody @Parameter(description = "Details of the currency exchange request") ExchangeRequest exchangeRequest) {
        return ResponseEntity.ok(service.exchangeCurrency(exchangeRequest));
    }
}