CREATE TYPE account_share_permission AS ENUM ('VIEW', 'EDIT');

CREATE TABLE account_shares (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    shared_with_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    permission account_share_permission NOT NULL DEFAULT 'VIEW',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(account_id, shared_with_user_id)
);

CREATE INDEX idx_account_shares_account_id ON account_shares(account_id);
CREATE INDEX idx_account_shares_shared_with_user_id ON account_shares(shared_with_user_id);

COMMENT ON TABLE account_shares IS 'Compartilhamento de contas entre usuários (família/casal) com níveis de permissão e auditoria';
