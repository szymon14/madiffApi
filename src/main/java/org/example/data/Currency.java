package org.example.data;

import lombok.Getter;

@Getter
public enum Currency {
    USD("USD"),
    PLN("PLN");

    private final String value;

    Currency(String value) {
        this.value = value;
    }
}