CREATE TABLE accounts (
    id          UUID PRIMARY KEY,
    name        TEXT NOT NULL,
    type        VARCHAR(20) NOT NULL,
    currency    VARCHAR(3) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL
);

CREATE TABLE transactions (
    id               UUID PRIMARY KEY,
    idempotency_key  TEXT NOT NULL UNIQUE,
    request_hash     VARCHAR(64) NOT NULL,
    description      TEXT,
    status           VARCHAR(20) NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL
);

-- Insert-only, high-volume: raw UUID FKs (no ORM object graph) to avoid
-- accidental N+1s. sequence_number is the real, monotonic insertion-order
-- cursor - entries.id is a random UUID and must never be used for ordering.
CREATE TABLE entries (
    id               UUID PRIMARY KEY,
    sequence_number  BIGSERIAL NOT NULL,
    transaction_id   UUID NOT NULL REFERENCES transactions (id),
    account_id       UUID NOT NULL REFERENCES accounts (id),
    amount           BIGINT NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL
);

CREATE INDEX entries_account_id_sequence_number_idx ON entries (account_id, sequence_number);
CREATE INDEX entries_transaction_id_idx ON entries (transaction_id);

-- Materialized cache of SUM(entries.amount) for one account, kept in sync in
-- the same DB transaction as the entry insert. The only row ever contended.
CREATE TABLE balances (
    account_id  UUID PRIMARY KEY REFERENCES accounts (id),
    balance     BIGINT NOT NULL DEFAULT 0,
    updated_at  TIMESTAMPTZ NOT NULL
);

-- USER accounts get a min_balance = 0 floor enforced at write time.
-- PLATFORM/REVENUE/CLEARING accounts have no floor (CLEARING specifically
-- needs to go negative transiently pre-settlement). This has to be a trigger,
-- not a plain CHECK, because it needs to look sideways at accounts.type.
CREATE FUNCTION enforce_user_balance_floor() RETURNS trigger AS $$
BEGIN
    IF NEW.balance < 0 AND EXISTS (
        SELECT 1 FROM accounts WHERE id = NEW.account_id AND type = 'USER'
    ) THEN
        RAISE EXCEPTION 'balance floor violated for account %', NEW.account_id
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_balances_floor
    BEFORE UPDATE ON balances
    FOR EACH ROW
    EXECUTE FUNCTION enforce_user_balance_floor();
