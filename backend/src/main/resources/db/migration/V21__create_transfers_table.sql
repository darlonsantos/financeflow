-- Transferências entre contas: apenas movimentam saldo (sem transações de despesa/receita)
CREATE TABLE transfers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    origin_account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    destination_account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    amount DECIMAL(15,2) NOT NULL CHECK (amount > 0),
    transfer_date DATE NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_transfers_user_id ON transfers(user_id);
CREATE INDEX idx_transfers_origin_account_id ON transfers(origin_account_id);
CREATE INDEX idx_transfers_destination_account_id ON transfers(destination_account_id);
CREATE INDEX idx_transfers_transfer_date ON transfers(transfer_date);
CREATE INDEX idx_transfers_created_at ON transfers(created_at);
