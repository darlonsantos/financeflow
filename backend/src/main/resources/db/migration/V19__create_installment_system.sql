-- Sistema avançado de parcelamento: compras parceladas, financiamentos e assinaturas recorrentes
CREATE TYPE installment_group_type AS ENUM ('FIXED', 'VARIABLE', 'RECURRING');
CREATE TYPE installment_group_status AS ENUM ('ACTIVE', 'PAID_OFF', 'CANCELLED');
CREATE TYPE installment_item_status AS ENUM ('PENDING', 'PAID', 'CANCELLED');
CREATE TYPE installment_history_action AS ENUM ('CREATED', 'PAY_INSTALLMENT', 'EARLY_SETTLEMENT', 'RENEGOTIATION', 'CANCELLED');

-- Grupo de parcelas (uma compra/financiamento/assinatura)
CREATE TABLE installment_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
    description VARCHAR(500),
    total_amount DECIMAL(15,2) NOT NULL CHECK (total_amount > 0),
    installment_type installment_group_type NOT NULL DEFAULT 'FIXED',
    status installment_group_status NOT NULL DEFAULT 'ACTIVE',
    first_due_date DATE NOT NULL,
    number_of_installments INTEGER NOT NULL CHECK (number_of_installments >= 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    version INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX idx_installment_groups_user_id ON installment_groups(user_id);
CREATE INDEX idx_installment_groups_account_id ON installment_groups(account_id);
CREATE INDEX idx_installment_groups_status ON installment_groups(status);
CREATE INDEX idx_installment_groups_first_due_date ON installment_groups(first_due_date);

-- Parcelas individuais
CREATE TABLE installment_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    installment_group_id UUID NOT NULL REFERENCES installment_groups(id) ON DELETE CASCADE,
    installment_number INTEGER NOT NULL CHECK (installment_number >= 1),
    due_date DATE NOT NULL,
    amount DECIMAL(15,2) NOT NULL CHECK (amount >= 0),
    status installment_item_status NOT NULL DEFAULT 'PENDING',
    transaction_id UUID REFERENCES transactions(id) ON DELETE SET NULL,
    paid_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(installment_group_id, installment_number)
);

CREATE INDEX idx_installment_items_group_id ON installment_items(installment_group_id);
CREATE INDEX idx_installment_items_due_date ON installment_items(due_date);
CREATE INDEX idx_installment_items_status ON installment_items(status);

-- Histórico para rastreabilidade (renegociação, quitação antecipada, etc.)
CREATE TABLE installment_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    installment_group_id UUID NOT NULL REFERENCES installment_groups(id) ON DELETE CASCADE,
    action installment_history_action NOT NULL,
    details JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_installment_history_group_id ON installment_history(installment_group_id);

COMMENT ON TABLE installment_groups IS 'Grupos de parcelamento: compras parceladas, financiamentos ou assinaturas recorrentes';
COMMENT ON TABLE installment_items IS 'Parcelas individuais; quando paga, vincula transaction_id';
COMMENT ON TABLE installment_history IS 'Auditoria de ações: criação, pagamento, quitação antecipada, renegociação';
