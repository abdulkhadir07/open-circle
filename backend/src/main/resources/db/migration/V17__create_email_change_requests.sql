CREATE UNIQUE INDEX uk_users_email_ci
    ON users (LOWER(email));

ALTER TABLE auth_sessions
    DROP CONSTRAINT chk_auth_sessions_revocation_reason;

ALTER TABLE auth_sessions
    ADD CONSTRAINT chk_auth_sessions_revocation_reason
        CHECK (revocation_reason IS NULL OR revocation_reason IN (
            'LOGOUT',
            'LOGOUT_ALL',
            'PASSWORD_RESET',
            'PASSWORD_CHANGE',
            'EMAIL_CHANGE',
            'USER_REVOKED',
            'REFRESH_TOKEN_REUSE'
        ));

CREATE TABLE email_change_requests (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    new_email VARCHAR(160) NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_email_change_requests_user
        FOREIGN KEY (user_id)
            REFERENCES users(id)
            ON DELETE CASCADE,
    CONSTRAINT chk_email_change_requests_email_not_blank
        CHECK (BTRIM(new_email) <> ''),
    CONSTRAINT chk_email_change_requests_email_normalized
        CHECK (new_email = LOWER(BTRIM(new_email))),
    CONSTRAINT chk_email_change_requests_code_hash_not_blank
        CHECK (BTRIM(code_hash) <> ''),
    CONSTRAINT chk_email_change_requests_expiry
        CHECK (expires_at > created_at),
    CONSTRAINT chk_email_change_requests_attempt_count
        CHECK (attempt_count BETWEEN 0 AND 10),
    CONSTRAINT chk_email_change_requests_used
        CHECK (used_at IS NULL OR used_at >= created_at)
);

CREATE UNIQUE INDEX uk_email_change_requests_active_user
    ON email_change_requests (user_id)
    WHERE used_at IS NULL;

CREATE INDEX idx_email_change_requests_user_created
    ON email_change_requests (user_id, created_at DESC);
