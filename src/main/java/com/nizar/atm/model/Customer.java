package com.nizar.atm.model;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

@Getter
@Setter
@ToString
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class Customer extends AbstractEntity {
    private String name;
    private BigDecimal balance;
    private String pinCode;
    private BigInteger accountNumber;
    private CustomerStatus status;
    private Date lastLogin;
    private Date createdAt;
    private Date updatedAt;
}