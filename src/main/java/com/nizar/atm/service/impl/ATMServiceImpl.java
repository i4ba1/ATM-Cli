package com.nizar.atm.service.impl;

import com.nizar.atm.model.Customer;
import com.nizar.atm.model.CustomerStatus;
import com.nizar.atm.service.ATMService;
import com.nizar.atm.service.CustomerService;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.Optional;


@AllArgsConstructor
public class ATMServiceImpl implements ATMService {
    private static final Logger logger = LoggerFactory.getLogger(ATMServiceImpl.class);
    private CustomerService customerService;
    private Customer currentSession;

    public ATMServiceImpl() {
        this.customerService = new CustomerServiceImpl();
    }

    @Override
    public String register(String name, BigDecimal initialBalance) {
        try {
            if (name == null || name.trim().isEmpty()) {
                return "Error: Name cannot be empty";
            }
            if (initialBalance == null || initialBalance.compareTo(BigDecimal.ZERO) < 0) {
                return "Error: Initial balance must be non-negative";
            }

            Customer customer = customerService.save(Customer.builder()
                    .name(name)
                    .balance(initialBalance)
                    .status(CustomerStatus.ACTIVE)
                    .build());

            return String.format("Registration successful!\nAccount Number: %s\nPIN: %s",
                    customer.getAccountNumber(), customer.getPinCode());

        } catch (Exception e) {
            logger.error("Registration failed for name: {}", name, e);
            return "Error: " + e.getMessage();
        }
    }

    @Override
    public String login(String name, String pin) {
        try {
            if (currentSession != null) {
                return "Error: Another user is already logged in";
            }

            Optional<Customer> customer = customerService.findByName(name);
            if (customer.isEmpty()) {
                return "Error: Customer not found";
            }

            Customer foundCustomer = customer.get();
            if (!foundCustomer.getPinCode().equals(pin)) {
                return "Error: Invalid PIN";
            }

            if (foundCustomer.getStatus() != CustomerStatus.ACTIVE) {
                return "Error: Account is not active";
            }

            currentSession = foundCustomer;
            foundCustomer.setLastLogin(new Date());
            customerService.save(foundCustomer);

            return String.format("Welcome %s!\nCurrent balance: $%.2f",
                    name, foundCustomer.getBalance());

        } catch (Exception e) {
            logger.error("Login failed for name: {}", name, e);
            return "Error: " + e.getMessage();
        }
    }

    @Override
    public String withdraw(BigDecimal amount) {
        try {
            if (currentSession == null) {
                return "Error: No active session";
            }

            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                return "Error: Invalid amount";
            }

            if (currentSession.getBalance().compareTo(amount) < 0) {
                return "Error: Insufficient funds";
            }

            BigDecimal newBalance = currentSession.getBalance().subtract(amount);
            currentSession.setBalance(newBalance);
            customerService.updateBalance(currentSession.getId(), newBalance);

            return String.format("Withdrawal successful!\nNew balance: $%.2f", newBalance);

        } catch (Exception e) {
            logger.error("Withdrawal failed for customer: {}", currentSession.getName(), e);
            return "Error: " + e.getMessage();
        }
    }

    @Override
    public String transfer(BigInteger targetAccount, BigDecimal amount) {
        try {
            if (currentSession == null) {
                return "Error: No active session";
            }

            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                return "Error: Invalid amount";
            }

            Optional<Customer> targetCustomer = customerService.findByAccountNumber(targetAccount);
            if (targetCustomer.isEmpty()) {
                return "Error: Target account not found";
            }

            if (currentSession.getBalance().compareTo(amount) < 0) {
                return "Error: Insufficient funds";
            }

            Customer recipient = targetCustomer.get();

            // Update sender's balance
            BigDecimal senderNewBalance = currentSession.getBalance().subtract(amount);
            currentSession.setBalance(senderNewBalance);
            customerService.updateBalance(currentSession.getId(), senderNewBalance);

            // Update recipient's balance
            BigDecimal recipientNewBalance = recipient.getBalance().add(amount);
            recipient.setBalance(recipientNewBalance);
            customerService.updateBalance(recipient.getId(), recipientNewBalance);

            return String.format("Transfer successful!\nNew balance: $%.2f", senderNewBalance);

        } catch (Exception e) {
            logger.error("Transfer failed for customer: {}", currentSession.getName(), e);
            return "Error: " + e.getMessage();
        }
    }

    public String logout() {
        try {
            if (currentSession == null) {
                return "Error: No active session";
            }

            currentSession = null;
            return "Logout successful!";

        } catch (Exception e) {
            logger.error("Logout failed", e);
            return "Error: " + e.getMessage();
        }
    }
}
