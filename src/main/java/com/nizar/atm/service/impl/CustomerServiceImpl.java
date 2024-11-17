package com.nizar.atm.service.impl;

import com.nizar.atm.model.Customer;
import com.nizar.atm.model.CustomerStatus;
import com.nizar.atm.repository.impl.CustomerRepositoryImpl;
import com.nizar.atm.service.CustomerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class CustomerServiceImpl implements CustomerService {
    private static final Logger logger = LoggerFactory.getLogger(CustomerServiceImpl.class);
    private final CustomerRepositoryImpl customerRepository;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public CustomerServiceImpl() {
        this.customerRepository = new CustomerRepositoryImpl();
    }

    @Override
    public Optional<Customer> findById(UUID id) throws Exception {
        try {
            logger.debug("Finding customer by ID: {}", id);
            Optional<Customer> customer = customerRepository.findById(id);

            if (customer.isPresent()) {
                logger.debug("Found customer: {}", customer.get().getName());
            } else {
                logger.debug("No customer found with ID: {}", id);
            }

            return customer;
        } catch (Exception e) {
            logger.error("Error finding customer by ID: {}", id, e);
            throw new Exception("Failed to find customer by ID: " + id, e);
        }
    }

    @Override
    public Optional<Customer> findByName(String name) throws Exception {
        if (name == null || name.trim().isEmpty()) {
            logger.warn("Attempted to find customer with null or empty name");
            throw new IllegalArgumentException("Customer name cannot be null or empty");
        }

        try {
            logger.debug("Finding customer by name: {}", name);
            Optional<Customer> customer = customerRepository.findByName(name);

            if (customer.isPresent()) {
                logger.debug("Found customer: {}", customer.get().getName());
            } else {
                logger.debug("No customer found with name: {}", name);
            }

            return customer;
        } catch (Exception e) {
            logger.error("Error finding customer by name: {}", name, e);
            throw new Exception("Failed to find customer by name: " + name, e);
        }
    }

    // Helper methods for generating account number and PIN
    private BigInteger generateUniqueAccountNumber() throws Exception {
        int maxAttempts = 10; // Prevent infinite loop
        int attempts = 0;

        while (attempts < maxAttempts) {
            // Generate 16-digit account number starting with 8
            StringBuilder accountNum = new StringBuilder("8");
            for (int i = 0; i < 15; i++) {
                accountNum.append(SECURE_RANDOM.nextInt(10));
            }

            BigInteger generatedNumber = new BigInteger(accountNum.toString());

            // Check if account number is unique
            if (customerRepository.findByAccountNumber(generatedNumber).isEmpty()) {
                return generatedNumber;
            }
            attempts++;
        }
        throw new Exception("Failed to generate unique account number after " + maxAttempts + " attempts");
    }

    private String generatePinCode() {
        // Generate 6-digit PIN
        StringBuilder pin = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            pin.append(SECURE_RANDOM.nextInt(10));
        }
        return pin.toString();
    }

    @Override
    public Customer save(Customer customer) throws Exception {
        validateCustomer(customer);

        try {
            logger.debug("Saving customer: {}", customer.getName());

            if (customer.getId() == null) {
                // This is a new customer, generate account number and PIN
                customer.setAccountNumber(generateUniqueAccountNumber());
                customer.setPinCode(generatePinCode());
                customer.setStatus(CustomerStatus.ACTIVE);
                customer.setCreatedAt(new Date());
                logger.debug("Generated new account number: {} for customer: {}",
                        customer.getAccountNumber(), customer.getName());
            } else {
                // This is an update, verify the customer exists
                Optional<Customer> existingCustomer = customerRepository.findById(customer.getId());
                if (existingCustomer.isEmpty()) {
                    logger.warn("Attempted to update non-existent customer: {}", customer.getId());
                    throw new Exception("Customer not found with ID: " + customer.getId());
                }

                // Preserve the original account number and PIN if they exist
                Customer existing = existingCustomer.get();
                if (customer.getAccountNumber() == null) {
                    customer.setAccountNumber(existing.getAccountNumber());
                }
                if (customer.getPinCode() == null) {
                    customer.setPinCode(existing.getPinCode());
                }
                if (customer.getCreatedAt() == null) {
                    customer.setCreatedAt(existing.getCreatedAt());
                }
            }

            // Always update the updatedAt timestamp
            customer.setUpdatedAt(new Date());

            // If status is not set, set it to ACTIVE
            if (customer.getStatus() == null) {
                customer.setStatus(CustomerStatus.ACTIVE);
            }

            Customer savedCustomer = customerRepository.save(customer);
            logger.debug("Successfully saved customer: {} with account number: {}",
                    savedCustomer.getId(), savedCustomer.getAccountNumber());

            return savedCustomer;

        } catch (Exception e) {
            logger.error("Error saving customer: {}", customer.getName(), e);
            throw new Exception("Failed to save customer: " + customer.getName(), e);
        }
    }

    private void validateCustomer(Customer customer) {
        if (customer == null) {
            throw new IllegalArgumentException("Customer cannot be null");
        }
        if (customer.getName() == null || customer.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be null or empty");
        }
        if (customer.getBalance() == null) {
            throw new IllegalArgumentException("Customer balance cannot be null");
        }
        if (customer.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Customer balance cannot be negative");
        }
    }

    @Override
    public void updateBalance(UUID id, BigDecimal newBalance) throws Exception {
        if (id == null) {
            logger.warn("Attempted to update balance with null ID");
            throw new IllegalArgumentException("Customer ID cannot be null");
        }

        if (newBalance == null) {
            logger.warn("Attempted to update with null balance for customer: {}", id);
            throw new IllegalArgumentException("New balance cannot be null");
        }

        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            logger.warn("Attempted to update with negative balance for customer: {}", id);
            throw new IllegalArgumentException("New balance cannot be negative");
        }

        try {
            logger.debug("Updating balance for customer: {} to {}", id, newBalance);

            // Verify customer exists before updating
            Optional<Customer> customer = customerRepository.findById(id);
            if (customer.isEmpty()) {
                logger.warn("Attempted to update balance for non-existent customer: {}", id);
                throw new Exception("Customer not found with ID: " + id);
            }

            customerRepository.updateBalance(id, newBalance);
            logger.debug("Successfully updated balance for customer: {}", id);

        } catch (Exception e) {
            logger.error("Error updating balance for customer: {}", id, e);
            throw new Exception("Failed to update balance for customer: " + id, e);
        }
    }

    @Override
    public List<Customer> findAll() throws Exception {
        try {
            logger.debug("Retrieving all customers");
            List<Customer> customers = customerRepository.findAll();
            logger.debug("Found {} customers", customers.size());
            return customers;

        } catch (Exception e) {
            logger.error("Error retrieving all customers", e);
            throw new Exception("Failed to retrieve all customers", e);
        }
    }

    @Override
    public Optional<Customer> findByAccountNumber(BigInteger accountNumber) throws Exception {
        // Validate the account number format before querying
        if (accountNumber == null) {
            logger.warn("Attempted to find customer with null account number");
            throw new IllegalArgumentException("Account number cannot be null");
        }

        // Validate the account number format before querying
        if (isValidAccountNumber(accountNumber)) {
            logger.warn("Invalid account number format: {}", accountNumber);
            throw new IllegalArgumentException("Invalid account number format");
        }

        try {
            logger.debug("Finding customer by account number: {}", accountNumber);
            Optional<Customer> customer = customerRepository.findByAccountNumber(accountNumber);

            if (customer.isPresent()) {
                logger.debug("Found customer: {} with account number: {}",
                        customer.get().getName(), accountNumber);
            } else {
                logger.debug("No customer found with account number: {}", accountNumber);
            }

            return customer;
        } catch (Exception e) {
            logger.error("Error finding customer by account number: {}", accountNumber, e);
            throw new Exception("Failed to find customer by account number: " + accountNumber, e);
        }
    }

    // You might also want to add these utility methods for account number validation

    private boolean isValidAccountNumber(BigInteger accountNumber) {
        if (accountNumber == null) {
            return true;
        }

        String accountStr = accountNumber.toString();

        // Check if account number:
        // 1. Is exactly 16 digits
        // 2. Starts with 8
        // 3. Contains only digits
        return accountStr.length() != 16 ||
                !accountStr.startsWith("8") ||
                !accountStr.matches("\\d+");
    }

    private boolean isExistingAccountNumber(BigInteger accountNumber) throws Exception {
        try {
            return customerRepository.findByAccountNumber(accountNumber).isPresent();
        } catch (Exception e) {
            logger.error("Error checking account number existence: {}", accountNumber, e);
            throw new Exception("Failed to check account number existence", e);
        }
    }

    public Optional<Customer> findByCardNumber(String cardNumber) throws Exception {
        if (cardNumber == null || cardNumber.trim().isEmpty()) {
            logger.warn("Attempted to find customer with null or empty card number");
            throw new IllegalArgumentException("Card number cannot be null or empty");
        }

        try {
            logger.debug("Finding customer by card number: {}", cardNumber);
            Optional<Customer> customer = customerRepository.findByCardNumber(cardNumber);

            if (customer.isPresent()) {
                logger.debug("Found customer: {}", customer.get().getName());
            } else {
                logger.debug("No customer found with card number: {}", cardNumber);
            }

            return customer;
        } catch (Exception e) {
            logger.error("Error finding customer by card number: {}", cardNumber, e);
            throw new Exception("Failed to find customer by card number", e);
        }
    }

    public void deleteCustomer(UUID id) throws Exception {
        if (id == null) {
            logger.warn("Attempted to delete customer with null ID");
            throw new IllegalArgumentException("Customer ID cannot be null");
        }

        try {
            logger.debug("Deleting customer: {}", id);

            // Verify customer exists before deleting
            Optional<Customer> customer = customerRepository.findById(id);
            if (customer.isEmpty()) {
                logger.warn("Attempted to delete non-existent customer: {}", id);
                throw new Exception("Customer not found with ID: " + id);
            }

            customerRepository.deleteById(id);
            logger.debug("Successfully deleted customer: {}", id);

        } catch (Exception e) {
            logger.error("Error deleting customer: {}", id, e);
            throw new Exception("Failed to delete customer: " + id, e);
        }
    }

    private void validateAccountNumber(BigInteger accountNumber) throws Exception {
        if (isValidAccountNumber(accountNumber)) {
            logger.warn("Invalid account number format: {}", accountNumber);
            throw new IllegalArgumentException("Invalid account number format");
        }

        if (!isExistingAccountNumber(accountNumber)) {
            logger.warn("Account number does not exist: {}", accountNumber);
            throw new Exception("Account number not found");
        }
    }
}