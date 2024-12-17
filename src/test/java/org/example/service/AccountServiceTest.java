package org.example.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;


import org.example.client.ExchangeClient;
import org.example.data.Account;
import org.example.data.Currency;
import org.example.data.ExchangeRequest;
import org.example.data.dto.AccountDTO;
import org.example.data.dto.NbpExchangeResponse;
import org.example.exception.AccountNotFound;
import org.example.exception.InvalidRequest;
import org.example.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.List;

class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private ExchangeClient exchangeClient;
    @InjectMocks
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void exchangeCurrencyPLNToUSDSuccess() {
        long accountId = 123L;
        double amount = 1000;
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(mockAccount()));
        when(accountRepository.save(any(AccountDTO.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(exchangeClient.getExchangeRate(Currency.USD)).thenReturn(mockNbpResponse());

        Account updatedAccount = accountService.exchangeCurrency(new ExchangeRequest(accountId, "PLN", "USD", amount));
        assertEquals(0, updatedAccount.getBalancePLN());
        assertEquals(500.0, updatedAccount.getBalanceUSD(), 0.01);
    }

    @Test
    void exchangeCurrencyInsufficientBalanceThrowsException() {
        long accountId = 123L;
        double amount = 3000;
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(mockAccount()));
        when(exchangeClient.getExchangeRate(Currency.USD)).thenReturn(mockNbpResponse());

        RuntimeException exception = assertThrows(InvalidRequest.class, () ->
                accountService.exchangeCurrency(new ExchangeRequest(accountId, "PLN", "USD", amount)));
        assertEquals("Insufficient PLN balance", exception.getMessage());
    }

    @Test
    void exchangeCurrencyInvalidCurrencyThrowsException() {
        long accountId = 123L;
        RuntimeException exception = assertThrows(InvalidRequest.class, () ->
                accountService.exchangeCurrency(new ExchangeRequest(accountId, "INVALID", "USD", 1000.0)));
        assertEquals("Invalid currency: INVALID", exception.getMessage());
    }

    @Test
    void createExceptionNoPLNThrowsException() {
        Account badAccountRequestMock = new Account();
        badAccountRequestMock.setFirstName("Test");
        badAccountRequestMock.setLastName("Test");
        badAccountRequestMock.setBalanceUSD(0.0);

        RuntimeException exception = assertThrows(InvalidRequest.class, () ->
                accountService.createAccount(badAccountRequestMock));
        assertEquals("Account PLN balance not specified", exception.getMessage());
    }

    @Test
    void createExceptionNoFirstNameThrowsException() {
        Account badAccountRequestMock = new Account();
        badAccountRequestMock.setLastName("Test");
        badAccountRequestMock.setBalanceUSD(0.0);
        badAccountRequestMock.setBalancePLN(0.0);

        RuntimeException exception = assertThrows(InvalidRequest.class, () ->
                accountService.createAccount(badAccountRequestMock));
        assertEquals("Account first name not specified", exception.getMessage());
    }

    @Test
    void createExceptionNoLastNameThrowsException() {
        Account badAccountRequestMock = new Account();
        badAccountRequestMock.setFirstName("Test");
        badAccountRequestMock.setBalanceUSD(0.0);
        badAccountRequestMock.setBalancePLN(0.0);

        RuntimeException exception = assertThrows(InvalidRequest.class, () ->
                accountService.createAccount(badAccountRequestMock));
        assertEquals("Account last name not specified", exception.getMessage());
    }

    @Test
    void exchangeCurrencyNoAccountThrowsException() {
        long accountId = 123L;
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());
        RuntimeException exception = assertThrows(AccountNotFound.class, () ->
                accountService.exchangeCurrency(new ExchangeRequest(accountId, "PLN", "USD", 1000.0)));
        assertEquals("Account not found", exception.getMessage());
    }

    @Test
    void getNonExistingAccountThrowsException() {
        long accountId = 123L;
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());
        RuntimeException exception = assertThrows(AccountNotFound.class, () ->
                accountService.getAccount(accountId));
        assertEquals("Account not found", exception.getMessage());
    }

    private NbpExchangeResponse mockNbpResponse(){
        NbpExchangeResponse response = new NbpExchangeResponse();
        NbpExchangeResponse.Rate rate = new NbpExchangeResponse.Rate();
        rate.setAsk(2.0);
        response.setRates(List.of(rate));
        return response;
    }

    private AccountDTO mockAccount(){
        AccountDTO accountDTO = new AccountDTO();
        accountDTO.setFirstName("John");
        accountDTO.setLastName("Doe");
        accountDTO.setBalancePLN(1000);
        accountDTO.setBalanceUSD(0);
        return accountDTO;
    }
}