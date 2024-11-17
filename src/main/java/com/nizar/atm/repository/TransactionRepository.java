package com.nizar.atm.repository;

import com.nizar.atm.model.Transaction;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository {
    Transaction save(Transaction transaction);
    List<Transaction> findByCustomerId(UUID customerId, int limit);
    List<Transaction> findByCustomerIdAndType(UUID customerId, String type, int limit);
    List<Transaction> findTransferHistory(UUID customerId, int limit);
}
