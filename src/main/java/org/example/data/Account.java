package org.example.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    private String firstName;
    private String lastName;
    private Double balancePLN;
    private Double balanceUSD;
}
