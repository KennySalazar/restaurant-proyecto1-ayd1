CREATE TABLE roles (
                       id BIGSERIAL PRIMARY KEY,
                       name VARCHAR(32) NOT NULL UNIQUE,
                       description VARCHAR(255) NOT NULL
);

CREATE TABLE app_users (
                           id BIGSERIAL PRIMARY KEY,
                           email VARCHAR(320) NOT NULL UNIQUE,
                           password_hash VARCHAR(100) NOT NULL,
                           enabled BOOLEAN NOT NULL DEFAULT TRUE,
                           token_version INTEGER NOT NULL DEFAULT 0,
                           version BIGINT NOT NULL DEFAULT 0,
                           role_id BIGINT NOT NULL REFERENCES roles (id),
                           created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                           CONSTRAINT app_users_email_lowercase
                               CHECK (email = lower(email)),

                           CONSTRAINT app_users_token_version_nonnegative
                               CHECK (token_version >= 0)
);