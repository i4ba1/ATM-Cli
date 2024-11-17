package com.nizar.atm.repository.impl;

import com.nizar.atm.config.DatabaseManager;
import com.nizar.atm.repository.CustomerRepository;
import com.nizar.atm.model.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class CustomerRepositoryImpl implements CustomerRepository {
    private static final Logger logger = LoggerFactory.getLogger(CustomerRepositoryImpl.class);
    private final DatabaseManager dbManager;

    private static final String FIND_BY_ID_SQL =
            "SELECT * FROM customers WHERE id = ?";

    private static final String FIND_BY_NAME_SQL =
            "SELECT * FROM customers WHERE name = ?";

    private static final String SAVE_SQL = """
        INSERT INTO customers (id, name, card_number, pin_hash, balance, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (id) DO UPDATE SET
            name = EXCLUDED.name,
            card_number = EXCLUDED.card_number,
            pin_hash = EXCLUDED.pin_hash,
            balance = EXCLUDED.balance,
            updated_at = EXCLUDED.updated_at
    """;

    private static final String UPDATE_BALANCE_SQL =
            "UPDATE customers SET balance = ?, updated_at = ? WHERE id = ?";

    private static final String FIND_ALL_SQL =
            "SELECT * FROM customers ORDER BY created_at DESC";

    private static final String FIND_BY_CARD_NUMBER_SQL =
            "SELECT * FROM customers WHERE card_number = ?";

    private static final String FIND_BY_ACCOUNT_NUMBER_SQL =
            "SELECT * FROM customers WHERE account_number = ?";

    public CustomerRepositoryImpl() {
        this.dbManager = DatabaseManager.getInstance();
    }

    private Customer mapResultSetToCustomer(ResultSet rs) throws SQLException {
        Customer customer = new Customer();
        customer.setId(UUID.fromString(rs.getString("id")));
        customer.setName(rs.getString("name"));
        customer.setBalance(rs.getBigDecimal("balance"));
        customer.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        customer.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return customer;
    }

    @Override
    public Optional<Customer> findById(UUID id) throws Exception {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(FIND_BY_ID_SQL)) {

            pstmt.setObject(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToCustomer(rs));
            }
            return Optional.empty();

        } catch (SQLException e) {
            logger.error("Error finding customer by ID: {}", id, e);
            throw new Exception("Error finding customer by ID: " + id, e);
        }
    }

    @Override
    public Optional<Customer> findByName(String name) throws Exception {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(FIND_BY_NAME_SQL)) {

            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToCustomer(rs));
            }
            return Optional.empty();

        } catch (SQLException e) {
            logger.error("Error finding customer by name: {}", name, e);
            throw new Exception("Error finding customer by name: " + name, e);
        }
    }

    @Override
    public Customer save(Customer customer) throws Exception {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(SAVE_SQL)) {
                if (customer.getId() == null) {
                    customer.setId(UUID.randomUUID());
                }

                Timestamp now = Timestamp.valueOf(LocalDateTime.now());
                if (customer.getCreatedAt() == null) {
                    customer.setCreatedAt(now.toLocalDateTime());
                }
                customer.setUpdatedAt(now.toLocalDateTime());

                pstmt.setObject(1, customer.getId());
                pstmt.setString(2, customer.getName());
                pstmt.setString(3, null);
                pstmt.setString(4, null);
                pstmt.setBigDecimal(5, customer.getBalance());
                pstmt.setTimestamp(6, Timestamp.valueOf(String.valueOf(customer.getCreatedAt())));
                pstmt.setTimestamp(7, Timestamp.valueOf(String.valueOf(customer.getUpdatedAt())));

                pstmt.executeUpdate();
                conn.commit();

                logger.debug("Successfully saved customer: {}", customer.getId());
                return customer;
            }
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    logger.error("Error rolling back transaction", ex);
                }
            }
            logger.error("Error saving customer: {}", customer.getName(), e);
            throw new Exception("Error saving customer: " + customer.getName(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    logger.error("Error resetting auto-commit", e);
                }
            }
        }
    }

    @Override
    public void updateBalance(UUID id, BigDecimal newBalance) throws Exception {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(UPDATE_BALANCE_SQL)) {
                pstmt.setBigDecimal(1, newBalance);
                pstmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                pstmt.setObject(3, id);

                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected == 0) {
                    throw new Exception("No customer found with ID: " + id);
                }

                conn.commit();
                logger.debug("Successfully updated balance for customer: {}", id);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    logger.error("Error rolling back transaction", ex);
                }
            }
            logger.error("Error updating balance for customer ID: {}", id, e);
            throw new Exception("Error updating balance for customer ID: " + id, e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    logger.error("Error resetting auto-commit", e);
                }
            }
        }
    }

    @Override
    public List<Customer> findAll() throws Exception {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(FIND_ALL_SQL)) {

            ResultSet rs = pstmt.executeQuery();
            List<Customer> customers = new ArrayList<>();

            while (rs.next()) {
                customers.add(mapResultSetToCustomer(rs));
            }

            return customers;

        } catch (SQLException e) {
            logger.error("Error retrieving all customers", e);
            throw new Exception("Error retrieving all customers", e);
        }
    }

    public Optional<Customer> findByCardNumber(String cardNumber) throws Exception {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(FIND_BY_CARD_NUMBER_SQL)) {

            pstmt.setString(1, cardNumber);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToCustomer(rs));
            }
            return Optional.empty();

        } catch (SQLException e) {
            logger.error("Error finding customer by card number: {}", cardNumber, e);
            throw new Exception("Error finding customer by card number", e);
        }
    }

    public void deleteById(UUID id) throws Exception {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(
                    "DELETE FROM customers WHERE id = ?")) {
                pstmt.setObject(1, id);
                pstmt.executeUpdate();
                conn.commit();
                logger.debug("Successfully deleted customer: {}", id);
            }
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    logger.error("Error rolling back transaction", ex);
                }
            }
            logger.error("Error deleting customer: {}", id, e);
            throw new Exception("Error deleting customer: " + id, e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    logger.error("Error resetting auto-commit", e);
                }
            }
        }
    }

    public Optional<Customer> findByAccountNumber(BigInteger accountNumber) throws Exception {
        if (accountNumber == null) {
            logger.warn("Attempted to find customer with null account number");
            throw new IllegalArgumentException("Account number cannot be null");
        }

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(FIND_BY_ACCOUNT_NUMBER_SQL)) {

            // Convert BigInteger to String for storage
            pstmt.setString(1, accountNumber.toString());

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Customer customer = mapResultSetToCustomer(rs);
                logger.debug("Found customer with account number: {}", accountNumber);
                return Optional.of(customer);
            }

            logger.debug("No customer found with account number: {}", accountNumber);
            return Optional.empty();

        } catch (SQLException e) {
            logger.error("Error finding customer by account number: {}", accountNumber, e);
            throw new Exception("Error finding customer by account number", e);
        }
    }
}