ALTER TABLE users
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN email_verified_at TIMESTAMP WITH TIME ZONE;

CREATE TABLE email_verification_codes (
                                          id UUID PRIMARY KEY,
                                          user_id UUID NOT NULL,
                                          code_hash VARCHAR(255) NOT NULL,
                                          expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                          attempt_count INTEGER NOT NULL DEFAULT 0,
                                          used_at TIMESTAMP WITH TIME ZONE,
                                          created_at TIMESTAMP WITH TIME ZONE NOT NULL,

                                          CONSTRAINT fk_email_verification_codes_user
                                              FOREIGN KEY (user_id)
                                                  REFERENCES users (id)
                                                  ON DELETE CASCADE,

                                          CONSTRAINT ck_email_verification_codes_attempt_count
                                              CHECK (attempt_count >= 0)
);

CREATE INDEX idx_email_verification_codes_user_id
    ON email_verification_codes (user_id);

CREATE INDEX idx_email_verification_codes_user_active
    ON email_verification_codes (user_id, used_at, expires_at);