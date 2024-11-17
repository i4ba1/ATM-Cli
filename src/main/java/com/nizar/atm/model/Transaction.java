package com.nizar.atm.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Transaction {
    private String transactionType;
    private UUID customerId;
    private UUID targetCustomerId;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String status;
    private String errorMessage;
    private UUID referenceId;
    private LocalDateTime createdAt;

    // Constructors, getters, setters
}
