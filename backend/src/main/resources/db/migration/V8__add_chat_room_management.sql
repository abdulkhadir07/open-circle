-- Adds room-level lifecycle state for saved chats, abandoned-room auto-close, and archived closure.
ALTER TABLE chat_rooms
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN saved_at TIMESTAMPTZ,
    ADD COLUMN saved_by_id UUID,
    ADD COLUMN auto_close_at TIMESTAMPTZ,
    ADD COLUMN closed_at TIMESTAMPTZ;

ALTER TABLE chat_rooms
    ADD CONSTRAINT fk_chat_rooms_saved_by
        FOREIGN KEY (saved_by_id) REFERENCES users(id),
    ADD CONSTRAINT chk_chat_rooms_status
        CHECK (status IN ('ACTIVE', 'CLOSED')),
    ADD CONSTRAINT chk_chat_rooms_closed_state
        CHECK (
            (status = 'ACTIVE' AND closed_at IS NULL)
            OR
            (status = 'CLOSED' AND closed_at IS NOT NULL)
        ),
    ADD CONSTRAINT chk_chat_rooms_saved_state
        CHECK (
            (saved_at IS NULL AND saved_by_id IS NULL)
            OR
            (saved_at IS NOT NULL AND saved_by_id IS NOT NULL)
        ),
    ADD CONSTRAINT chk_chat_rooms_saved_rooms_do_not_auto_close
        CHECK (saved_at IS NULL OR auto_close_at IS NULL),
    ADD CONSTRAINT chk_chat_rooms_closed_rooms_do_not_auto_close
        CHECK (status <> 'CLOSED' OR auto_close_at IS NULL),
    ADD CONSTRAINT chk_chat_rooms_auto_close_after_created
        CHECK (auto_close_at IS NULL OR auto_close_at >= created_at),
    ADD CONSTRAINT chk_chat_rooms_saved_after_created
        CHECK (saved_at IS NULL OR saved_at >= created_at),
    ADD CONSTRAINT chk_chat_rooms_closed_after_created
        CHECK (closed_at IS NULL OR closed_at >= created_at);

-- Adds participant-level state for leaving, poster removal, and delete-for-me visibility.
ALTER TABLE chat_room_participants
    ADD COLUMN left_at TIMESTAMPTZ,
    ADD COLUMN removed_at TIMESTAMPTZ,
    ADD COLUMN removed_by_id UUID,
    ADD COLUMN hidden_at TIMESTAMPTZ;

ALTER TABLE chat_room_participants
    ADD CONSTRAINT fk_chat_room_participants_removed_by
        FOREIGN KEY (removed_by_id) REFERENCES users(id),
    ADD CONSTRAINT chk_chat_room_participants_removed_state
        CHECK (
            (removed_at IS NULL AND removed_by_id IS NULL)
            OR
            (removed_at IS NOT NULL AND removed_by_id IS NOT NULL)
        ),
    ADD CONSTRAINT chk_chat_room_participants_single_exit
        CHECK (left_at IS NULL OR removed_at IS NULL),
    ADD CONSTRAINT chk_chat_room_participants_left_after_joined
        CHECK (left_at IS NULL OR left_at >= joined_at),
    ADD CONSTRAINT chk_chat_room_participants_removed_after_joined
        CHECK (removed_at IS NULL OR removed_at >= joined_at),
    ADD CONSTRAINT chk_chat_room_participants_hidden_after_joined
        CHECK (hidden_at IS NULL OR hidden_at >= joined_at);

CREATE INDEX idx_chat_rooms_auto_close
    ON chat_rooms(auto_close_at)
    WHERE status = 'ACTIVE'
      AND saved_at IS NULL
      AND auto_close_at IS NOT NULL;

CREATE INDEX idx_chat_room_participants_room_active
    ON chat_room_participants(chat_room_id)
    WHERE left_at IS NULL
      AND removed_at IS NULL;

CREATE INDEX idx_chat_room_participants_user_accessible
    ON chat_room_participants(user_id, chat_room_id)
    WHERE hidden_at IS NULL
      AND left_at IS NULL
      AND removed_at IS NULL;