-- Moedas suportadas (lista configurável; taxas de conversão não alteram valores históricos)
CREATE TABLE currencies (
    code VARCHAR(3) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    symbol VARCHAR(10) NOT NULL,
    decimal_places INTEGER NOT NULL DEFAULT 2
);

-- Dados iniciais: moedas comuns (inserir ANTES de referenciar em accounts)
INSERT INTO currencies (code, name, symbol, decimal_places) VALUES
    ('BRL', 'Real Brasileiro', 'R$', 2),
    ('USD', 'Dólar Americano', 'US$', 2),
    ('EUR', 'Euro', '€', 2),
    ('GBP', 'Libra Esterlina', '£', 2);

-- Taxas de conversão: 1 from_currency = rate * to_currency (ex: 1 USD = 5.20 BRL)
-- effective_at permite histórico de cotações; usa-se a mais recente <= momento da conversão
CREATE TABLE currency_rates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    from_currency_code VARCHAR(3) NOT NULL REFERENCES currencies(code) ON DELETE CASCADE,
    to_currency_code VARCHAR(3) NOT NULL REFERENCES currencies(code) ON DELETE CASCADE,
    rate DECIMAL(18, 8) NOT NULL CHECK (rate > 0),
    effective_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(from_currency_code, to_currency_code, effective_at)
);

CREATE INDEX idx_currency_rates_from_to ON currency_rates(from_currency_code, to_currency_code);
CREATE INDEX idx_currency_rates_effective_at ON currency_rates(effective_at DESC);

-- Taxas exemplo (1 unidade da moeda = rate na moeda destino); usuário pode configurar depois
INSERT INTO currency_rates (from_currency_code, to_currency_code, rate, effective_at) VALUES
    ('BRL', 'BRL', 1.00000000, CURRENT_TIMESTAMP),
    ('USD', 'USD', 1.00000000, CURRENT_TIMESTAMP),
    ('EUR', 'EUR', 1.00000000, CURRENT_TIMESTAMP),
    ('GBP', 'GBP', 1.00000000, CURRENT_TIMESTAMP),
    ('USD', 'BRL', 5.20000000, CURRENT_TIMESTAMP),
    ('BRL', 'USD', 0.19230769, CURRENT_TIMESTAMP),
    ('EUR', 'BRL', 5.60000000, CURRENT_TIMESTAMP),
    ('BRL', 'EUR', 0.17857143, CURRENT_TIMESTAMP),
    ('GBP', 'BRL', 6.50000000, CURRENT_TIMESTAMP),
    ('BRL', 'GBP', 0.15384615, CURRENT_TIMESTAMP),
    ('EUR', 'USD', 1.08000000, CURRENT_TIMESTAMP),
    ('USD', 'EUR', 0.92592593, CURRENT_TIMESTAMP),
    ('GBP', 'USD', 1.27000000, CURRENT_TIMESTAMP),
    ('USD', 'GBP', 0.78740157, CURRENT_TIMESTAMP),
    ('EUR', 'GBP', 0.85000000, CURRENT_TIMESTAMP),
    ('GBP', 'EUR', 1.17647059, CURRENT_TIMESTAMP);

-- Conta passa a ter moeda; saldos e movimentações permanecem na moeda da conta (valores históricos inalterados)
-- BRL já existe em currencies, então a FK é satisfeita
ALTER TABLE accounts ADD COLUMN currency_code VARCHAR(3) NOT NULL DEFAULT 'BRL' REFERENCES currencies(code) ON DELETE RESTRICT;
CREATE INDEX idx_accounts_currency_code ON accounts(currency_code);

COMMENT ON TABLE currency_rates IS 'Taxas de conversão configuráveis; valores históricos de transações/contas não são alterados';
