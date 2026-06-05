CREATE TABLE IF NOT EXISTS transactions (
    id BIGSERIAL PRIMARY KEY,
    sepay_id VARCHAR(100) NOT NULL UNIQUE,
    gateway VARCHAR(100),
    transaction_date VARCHAR(100),
    account_number VARCHAR(50),
    sub_account VARCHAR(100) NOT NULL DEFAULT '',
    code VARCHAR(100),
    amount_in BIGINT NOT NULL DEFAULT 0,
    amount_out BIGINT NOT NULL DEFAULT 0,
    accumulated BIGINT NOT NULL DEFAULT 0,
    content TEXT,
    reference_code VARCHAR(100) NOT NULL DEFAULT '',
    body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
