ALTER TABLE users
    ADD COLUMN hidden_chats_pin_hash VARCHAR(255),
    ADD COLUMN hidden_chats_pin_failed_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN hidden_chats_pin_locked_until TIMESTAMP WITH TIME ZONE;
