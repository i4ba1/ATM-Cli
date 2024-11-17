package com.nizar.atm.service;

import com.nizar.atm.model.Customer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerService {
    Optional<Customer> findById(UUID id) throws Exception;
    Optional<Customer> findByName(String name) throws Exception;
    Customer save(Customer customer) throws Exception;
    void updateBalance(UUID id, BigDecimal newBalance) throws Exception;
    List<Customer> findAll() throws Exception;
    Optional<Customer> findByAccountNumber(BigInteger accountNumber) throws Exception;
}
