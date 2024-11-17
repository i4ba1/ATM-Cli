-- src/main/resources/db/queries.sql

-- Check latest balance
SELECT balance
FROM customers
WHERE id = ? AND status = 'ACTIVE';

-- Get transaction history (generic)
SELECT t.*,
       c.name as customer_name,
       tc.name as target_customer_name
FROM transactions t
         LEFT JOIN customers c ON t.customer_id = c.id
         LEFT JOIN customers tc ON t.target_customer_id = tc.id
WHERE t.customer_id = ?
ORDER BY t.created_at DESC
    LIMIT ?;

-- Get deposit history
SELECT *
FROM transactions
WHERE customer_id = ?
  AND transaction_type = 'DEPOSIT'
  AND status = 'SUCCESS'
ORDER BY created_at DESC;

-- Get withdrawal history
SELECT *
FROM transactions
WHERE customer_id = ?
  AND transaction_type = 'WITHDRAW'
  AND status = 'SUCCESS'
ORDER BY created_at DESC;

-- Get transfer history (sent and received)
SELECT t.*,
       c.name as from_customer,
       tc.name as to_customer
FROM transactions t
         JOIN customers c ON t.customer_id = c.id
         JOIN customers tc ON t.target_customer_id = tc.id
WHERE (t.customer_id = ? OR t.target_customer_id = ?)
  AND t.transaction_type = 'TRANSFER'
  AND t.status = 'SUCCESS'
ORDER BY t.created_at DESC;

-- Get active debts
SELECT d.*,
       c.name as creditor_name
FROM debts d
         JOIN customers c ON d.creditor_id = c.id
WHERE d.debtor_id = ?
  AND d.status = 'ACTIVE';

-- Get monthly statement
SELECT
    DATE_TRUNC('month', created_at) as month,
    transaction_type,
    COUNT(*) as transaction_count,
    SUM(CASE WHEN status = 'SUCCESS' THEN amount ELSE 0 END) as total_amount
FROM transactions
WHERE customer_id = ?
  AND created_at BETWEEN ? AND ?
GROUP BY DATE_TRUNC('month', created_at), transaction_type
ORDER BY month DESC;

-- Get daily transaction summary
SELECT
    DATE_TRUNC('day', created_at) as date,
    transaction_type,
    COUNT(*) as transaction_count,
    SUM(CASE WHEN status = 'SUCCESS' THEN amount ELSE 0 END) as total_amount
FROM transactions
WHERE customer_id = ?
  AND created_at >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY DATE_TRUNC('day', created_at), transaction_type
ORDER BY date DESC;