SET search_path TO ecofy_users;

CREATE TABLE IF NOT EXISTS idempotency_key (
    id uuid PRIMARY KEY,
    operation varchar(120) NOT NULL,
    idem_key varchar(200) NOT NULL,
    request_hash varchar(80) NOT NULL,
    expires_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL,
    CONSTRAINT uk_idempotency_operation_key UNIQUE (operation, idem_key)
);

CREATE INDEX IF NOT EXISTS idx_idempotency_expires_at ON idempotency_key(expires_at);
