SET search_path TO ecofy_users;

CREATE TABLE IF NOT EXISTS user_preference (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    pref_key varchar(60) NOT NULL,
    pref_value text NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT uk_user_preference_user_key UNIQUE (user_id, pref_key)
);

CREATE INDEX IF NOT EXISTS idx_user_preference_user_id ON user_preference(user_id);
