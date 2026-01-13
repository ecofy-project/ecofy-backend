SET search_path TO ecofy_users;

CREATE TABLE IF NOT EXISTS user_profile (
    id uuid PRIMARY KEY,
    external_auth_id varchar(200) NOT NULL UNIQUE,
    full_name varchar(200),
    email varchar(200),
    phone varchar(50),
    status varchar(20) NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_user_profile_email ON user_profile(email);
