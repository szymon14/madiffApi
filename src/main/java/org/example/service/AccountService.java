package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.client.ExchangeClient;
import org.example.data.Account;
import org.example.data.Currency;
import org.example.data.ExchangeRequest;
import org.example.data.dto.AccountDTO;
import org.example.data.dto.NbpExchangeResponse;
import org.example.exception.AccountNotFound;
import org.example.exception.InvalidRequest;
import org.example.mapper.AccountMapper;
import org.example.repository.AccountRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository repository;
    private final ExchangeClient exchangeClient;

    public AccountDTO createAccount(Account account) {
        validateAccountData(account);
        log.info("Creating a new account for user: {} {}", account.getFirstName(), account.getLastName());
        BigDecimal roundedBalancePLN = BigDecimal.valueOf(account.getBalancePLN())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal roundedBalanceUSD = BigDecimal.ZERO;
        if(account.getBalanceUSD() != null) {
            roundedBalanceUSD = BigDecimal.valueOf(account.getBalanceUSD())
                .setScale(2, RoundingMode.HALF_UP);
        }
        account.setBalancePLN(roundedBalancePLN.doubleValue());
        account.setBalanceUSD(roundedBalanceUSD.doubleValue());
        AccountDTO accountDTO = AccountMapper.INSTANCE.accountToAccountDTO(account);
        AccountDTO savedAccount = repository.save(accountDTO);
        log.info("Account created successfully with ID: {}", savedAccount.getId());
        return savedAccount;
    }

    public Account getAccount(Long id) {
        log.info("Fetching account with ID: {}", id);
        Optional<AccountDTO> accountDTO = repository.findById(id);
        if (accountDTO.isPresent()) {
            log.info("Account found for ID: {}", id);
            return AccountMapper.INSTANCE.accountDTOToAccount(accountDTO.get());
        } else {
            log.error("Account not found for ID: {}", id);
            throw new AccountNotFound("Account not found");
        }
    }

    public Account exchangeCurrency(ExchangeRequest exchangeRequest) {
        log.info("Processing currency exchange for Account ID: {}, From: {}, To: {}, Amount: {}",
                exchangeRequest.getAccountId(), exchangeRequest.getCurrencyFrom(),
                exchangeRequest.getCurrencyTo(), exchangeRequest.getAmount());

        validateAmount(exchangeRequest);
        Currency from = validateCurrency(exchangeRequest.getCurrencyFrom());
        Currency to = validateCurrency(exchangeRequest.getCurrencyTo());
        AccountDTO account = repository.findById(exchangeRequest.getAccountId())
                .orElseThrow(() -> {
                    log.error("Account not found for ID: {}", exchangeRequest.getAccountId());
                    return new AccountNotFound("Account not found");
                });

        NbpExchangeResponse exchangeResponse = exchangeClient.getExchangeRate(Currency.USD);
        BigDecimal bid = BigDecimal.valueOf(exchangeResponse.getRates().get(0).getBid());
        BigDecimal ask = BigDecimal.valueOf(exchangeResponse.getRates().get(0).getAsk());
        BigDecimal amount = BigDecimal.valueOf(exchangeRequest.getAmount()).setScale(2, RoundingMode.HALF_UP);

        if (from == Currency.PLN && to == Currency.USD) {
            handleFromPlnToUsd(account,amount,ask);
        }
        if (from == Currency.USD && to == Currency.PLN) {
           handleFromUsdToPln(account,amount,bid);
        }

        AccountDTO updatedAccount = repository.save(account);
        log.info("Currency exchange successful for Account ID: {}", exchangeRequest.getAccountId());
        return AccountMapper.INSTANCE.accountDTOToAccount(updatedAccount);
    }

    private void handleFromPlnToUsd(AccountDTO account, BigDecimal amount, BigDecimal ask){
        if (BigDecimal.valueOf(account.getBalancePLN()).compareTo(amount) < 0) {
            log.error("Insufficient PLN balance for Account ID: {}", account.getId());
            throw new InvalidRequest("Insufficient PLN balance");
        }
        BigDecimal usdAmount = amount.divide(ask, 2, RoundingMode.HALF_UP);
        account.setBalancePLN(BigDecimal.valueOf(account.getBalancePLN()).subtract(amount).setScale(2, RoundingMode.HALF_UP).doubleValue());
        account.setBalanceUSD(BigDecimal.valueOf(account.getBalanceUSD()).add(usdAmount).setScale(2, RoundingMode.HALF_UP).doubleValue());
        log.info("Converted {} PLN to {} USD for Account ID: {}", amount, usdAmount, account.getId());
    }

    private void handleFromUsdToPln(AccountDTO account, BigDecimal amount, BigDecimal bid){
        if (BigDecimal.valueOf(account.getBalanceUSD()).compareTo(amount) < 0) {
            log.error("Insufficient USD balance for Account ID: {}", account.getId());
            throw new InvalidRequest("Insufficient USD balance");
        }
        BigDecimal plnAmount = amount.multiply(bid).setScale(2, RoundingMode.HALF_UP);
        account.setBalanceUSD(BigDecimal.valueOf(account.getBalanceUSD()).subtract(amount).setScale(2, RoundingMode.HALF_UP).doubleValue());
        account.setBalancePLN(BigDecimal.valueOf(account.getBalancePLN()).add(plnAmount).setScale(2, RoundingMode.HALF_UP).doubleValue());
        log.info("Converted {} USD to {} PLN for Account ID: {}", amount, plnAmount, account.getId());
    }
    private Currency validateCurrency(String currency) {
        try {
            return Currency.valueOf(currency.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Invalid currency provided: {}", currency);
            throw new InvalidRequest("Invalid currency: " + currency);
        }
    }

    private void validateAmount(ExchangeRequest exchangeRequest){
        if(exchangeRequest.getAmount() == null){
            throw new InvalidRequest("Amount not specified");
        }
        if(exchangeRequest.getAmount() < 0.01){
            throw new InvalidRequest("Specified amount to small");
        }
    }

    private void validateAccountData(Account account){
        if(account.getBalancePLN() == null){
            throw new InvalidRequest("Account PLN balance not specified");
        }
        if(account.getFirstName() == null){
            throw new InvalidRequest("Account first name not specified");
        }
        if(account.getLastName() == null){
            throw new InvalidRequest("Account last name not specified");
        }
    }
}
