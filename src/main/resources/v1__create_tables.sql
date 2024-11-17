-- src/main/resources/db/v1__create_tables.sql

-- Add some useful indices and audit columns
-- src/main/resources/db/schema.sql

-- Customers table
CREATE TABLE IF NOT EXISTS customers (
                                         id UUID PRIMARY KEY,
                                         name VARCHAR NOT NULL UNIQUE,
                                         balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    status VARCHAR NOT NULL DEFAULT 'ACTIVE',
    last_login TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Debts table
CREATE TABLE IF NOT EXISTS debts (
                                     id UUID PRIMARY KEY,
                                     debtor_id UUID NOT NULL,
                                     creditor_id UUID NOT NULL,
                                     amount DECIMAL(15,2) NOT NULL,
    status VARCHAR NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (debtor_id) REFERENCES customers(id),
    FOREIGN KEY (creditor_id) REFERENCES customers(id)
    );

-- Transactions table
CREATE TABLE IF NOT EXISTS transactions (
                                            id UUID PRIMARY KEY,
                                            transaction_type VARCHAR NOT NULL,
                                            customer_id UUID NOT NULL,
                                            target_customer_id UUID,
                                            amount DECIMAL(15,2),
    balance_before DECIMAL(15,2),
    balance_after DECIMAL(15,2),
    status VARCHAR NOT NULL,
    error_message VARCHAR,
    reference_id UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    FOREIGN KEY (target_customer_id) REFERENCES customers(id)
    );

-- Indexes for better performance
CREATE INDEX IF NOT EXISTS idx_transactions_customer ON transactions(customer_id, created_at);
CREATE INDEX IF NOT EXISTS idx_transactions_type ON transactions(transaction_type, created_at);
CREATE INDEX IF NOT EXISTS idx_debts_debtor ON debts(debtor_id, status);