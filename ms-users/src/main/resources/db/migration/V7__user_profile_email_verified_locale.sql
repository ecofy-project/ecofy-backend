SET search_path TO ecofy_users;

-- Persiste campos de perfil vindos do ms-auth que antes eram perdidos no save/load:
--   - email_verified: fonte de verdade do Auth
--   - locale: preferência de idioma do usuário
ALTER TABLE user_profile
    ADD COLUMN IF NOT EXISTS email_verified boolean NOT NULL DEFAULT false;

ALTER TABLE user_profile
    ADD COLUMN IF NOT EXISTS locale varchar(20);
