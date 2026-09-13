CREATE TABLE auth_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    user_agent VARCHAR(512),
    created_at TIMESTAMPTZ NOT NULL,
    last_used_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    revocation_reason VARCHAR(40),

    CONSTRAINT fk_auth_sessions_user
        FOREIGN KEY (user_id)
            REFERENCES users(id)
            ON DELETE CASCADE,
    CONSTRAINT chk_auth_sessions_expiry
        CHECK (expires_at > created_at),
    CONSTRAINT chk_auth_sessions_last_used
        CHECK (last_used_at >= created_at AND last_used_at <= expires_at),
    CONSTRAINT chk_auth_sessions_revocation
        CHECK (
            (revoked_at IS NULL AND revocation_reason IS NULL)
            OR
            (revoked_at IS NOT NULL AND revocation_reason IS NOT NULL)
        ),
    CONSTRAINT chk_auth_sessions_revocation_reason
        CHECK (revocation_reason IS NULL OR revocation_reason IN (
            'LOGOUT',
            'PASSWORD_RESET',
            'REFRESH_TOKEN_REUSE'
        ))
);

CREATE INDEX idx_auth_sessions_active_user
    ON auth_sessions (user_id, last_used_at DESC)
    WHERE revoked_at IS NULL;

CREATE INDEX idx_auth_sessions_cleanup
    ON auth_sessions (expires_at, revoked_at);

CREATE TABLE session_refresh_tokens (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,

    CONSTRAINT fk_session_refresh_tokens_session
        FOREIGN KEY (session_id)
            REFERENCES auth_sessions(id)
            ON DELETE CASCADE,
    CONSTRAINT uk_session_refresh_tokens_hash
        UNIQUE (token_hash),
    CONSTRAINT chk_session_refresh_tokens_hash_format
        CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT chk_session_refresh_tokens_expiry
        CHECK (expires_at > issued_at),
    CONSTRAINT chk_session_refresh_tokens_used
        CHECK (used_at IS NULL OR (used_at >= issued_at AND used_at <= expires_at))
);

CREATE UNIQUE INDEX uk_session_refresh_tokens_current
    ON session_refresh_tokens (session_id)
    WHERE used_at IS NULL;

CREATE INDEX idx_session_refresh_tokens_session
    ON session_refresh_tokens (session_id, issued_at DESC);
