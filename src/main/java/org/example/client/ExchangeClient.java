package org.example.client;

import org.example.data.Currency;
import org.example.data.dto.NbpExchangeResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ExchangeClient {

    private static final String NBP_API_URL_TEMPLATE = "https://api.nbp.pl/api/exchangerates/rates/c/{currency}/today/";
    private final RestTemplate restTemplate;

    public ExchangeClient() {
        this.restTemplate = new RestTemplate();
    }

    public NbpExchangeResponse getExchangeRate(Currency currency) {
        String url = NBP_API_URL_TEMPLATE.replace("{currency}", currency.getValue().toLowerCase());
        return restTemplate.getForObject(url, NbpExchangeResponse.class);
    }
}