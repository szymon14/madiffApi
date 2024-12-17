package org.example.data;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExchangeRequest {
    private Long accountId;
    private String currencyFrom;
    private String currencyTo;
    private Double amount;
}
