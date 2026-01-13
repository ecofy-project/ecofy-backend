SET search_path TO ecofy_users;

CREATE TABLE IF NOT EXISTS linked_account (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    provider varchar(40) NOT NULL,
    external_account_ref varchar(200) NOT NULL,
    active boolean NOT NULL,
    linked_at timestamptz NOT NULL,
    CONSTRAINT uk_linked_account_user_provider_ref UNIQUE (user_id, provider, external_account_ref)
);

CREATE INDEX IF NOT EXISTS idx_linked_account_user_id ON linked_account(user_id);
