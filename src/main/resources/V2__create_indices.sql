-- src/main/resources/db/schema/V2__create_indices.sql
-- Indexes for customers table
CREATE INDEX IF NOT EXISTS idx_customers_name ON customers(name);
CREATE INDEX IF NOT EXISTS idx_customers_status ON customers(status);

-- Indexes for transactions table
CREATE INDEX IF NOT EXISTS idx_transactions_customer ON transactions(customer_id, created_at);
CREATE INDEX IF NOT EXISTS idx_transactions_target ON transactions(target_customer_id, created_at);
CREATE INDEX IF NOT EXISTS idx_transactions_type ON transactions(transaction_type);
CREATE INDEX IF NOT EXISTS idx_transactions_status ON transactions(status);

-- Indexes for debts table
CREATE INDEX IF NOT EXISTS idx_debts_debtor ON debts(debtor_id, status);
CREATE INDEX IF NOT EXISTS idx_debts_creditor ON debts(creditor_id, status);

-- Indexes for transaction_details table
CREATE INDEX IF NOT EXISTS idx_transaction_details_transaction ON transaction_details(transaction_id);