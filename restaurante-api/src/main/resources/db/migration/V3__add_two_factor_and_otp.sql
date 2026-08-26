ALTER TABLE app_users
    ADD COLUMN two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE otp_challenges (
                                id UUID PRIMARY KEY,

                                user_id BIGINT NOT NULL
                                    REFERENCES app_users (id)
                                        ON DELETE CASCADE,

                                purpose VARCHAR(32) NOT NULL,

                                code_hash VARCHAR(100) NOT NULL,

                                expires_at TIMESTAMPTZ NOT NULL,

                                consumed_at TIMESTAMPTZ,

                                attempts INTEGER NOT NULL DEFAULT 0,

                                created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                CONSTRAINT otp_challenges_attempts_nonnegative
                                    CHECK (attempts >= 0),

                                CONSTRAINT otp_challenges_purpose_valid
                                    CHECK (
                                        purpose IN (
                                                    'LOGIN',
                                                    'PASSWORD_RECOVERY',
                                                    'TWO_FACTOR_ENABLE',
                                                    'TWO_FACTOR_DISABLE'
                                            )
                                        )
);

CREATE INDEX idx_otp_challenges_user_purpose
    ON otp_challenges (user_id, purpose);

CREATE INDEX idx_otp_challenges_expires_at
    ON otp_challenges (expires_at);