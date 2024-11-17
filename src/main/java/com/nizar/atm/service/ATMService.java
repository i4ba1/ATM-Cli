package com.nizar.atm.service;

import java.math.BigDecimal;
import java.math.BigInteger;

public interface ATMService {
    String register(String name, BigDecimal initialBalance);
    String login(String name, String pin);
    String withdraw(BigDecimal amount);
    String transfer(BigInteger targetAccount, BigDecimal amount);
    String logout();
}
