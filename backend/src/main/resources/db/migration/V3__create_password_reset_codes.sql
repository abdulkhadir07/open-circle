CREATE TABLE password_reset_codes (
                                      id UUID PRIMARY KEY,
                                      user_id UUID NOT NULL,
                                      code_hash VARCHAR(255) NOT NULL,
                                      expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                      attempt_count INTEGER NOT NULL DEFAULT 0,
                                      used_at TIMESTAMP WITH TIME ZONE,
                                      created_at TIMESTAMP WITH TIME ZONE NOT NULL,

                                      CONSTRAINT fk_password_reset_codes_user
                                          FOREIGN KEY (user_id)
                                              REFERENCES users (id)
                                              ON DELETE CASCADE,

                                      CONSTRAINT ck_password_reset_codes_attempt_count
                                          CHECK (attempt_count >= 0)
);

CREATE INDEX idx_password_reset_codes_user_active
    ON password_reset_codes (user_id, used_at, expires_at);