-- Data de vencimento (opcional) para transações (ex.: contas a pagar)
ALTER TABLE transactions ADD COLUMN due_date DATE NULL;
CREATE INDEX idx_transactions_due_date ON transactions(due_date) WHERE due_date IS NOT NULL;
