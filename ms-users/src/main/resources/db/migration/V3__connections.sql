SET search_path TO ecofy_users;

CREATE TABLE IF NOT EXISTS connection (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    type varchar(30) NOT NULL,
    provider varchar(40) NOT NULL,
    metadata_json text,
    created_at timestamptz NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_connection_user_id ON connection(user_id);
