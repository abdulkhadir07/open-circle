-- Chat rooms are created from accepted engagement requests, not manually by users.
CREATE TABLE chat_rooms (
                            id UUID PRIMARY KEY,
                            invite_post_id UUID NOT NULL,
                            created_at TIMESTAMPTZ NOT NULL,
                            updated_at TIMESTAMPTZ NOT NULL,

                            CONSTRAINT uk_chat_rooms_invite_post UNIQUE (invite_post_id),
                            CONSTRAINT fk_chat_rooms_invite_post
                                FOREIGN KEY (invite_post_id) REFERENCES invite_posts(id),
                            CONSTRAINT chk_chat_rooms_timestamps
                                CHECK (updated_at >= created_at)
);

-- Each row represents one user who can access a chat room.
CREATE TABLE chat_room_participants (
                                        id UUID PRIMARY KEY,
                                        chat_room_id UUID NOT NULL,
                                        user_id UUID NOT NULL,
                                        joined_at TIMESTAMPTZ NOT NULL,

                                        CONSTRAINT uk_chat_room_participants_room_user UNIQUE (chat_room_id, user_id),
                                        CONSTRAINT fk_chat_room_participants_room
                                            FOREIGN KEY (chat_room_id) REFERENCES chat_rooms(id) ON DELETE CASCADE,
                                        CONSTRAINT fk_chat_room_participants_user
                                            FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Messages are stored only for users who belong to the room they are posting in.
CREATE TABLE chat_messages (
                               id UUID PRIMARY KEY,
                               chat_room_id UUID NOT NULL,
                               sender_id UUID NOT NULL,
                               body VARCHAR(1000) NOT NULL,
                               created_at TIMESTAMPTZ NOT NULL,

                               CONSTRAINT fk_chat_messages_room
                                   FOREIGN KEY (chat_room_id) REFERENCES chat_rooms(id) ON DELETE CASCADE,
                               CONSTRAINT fk_chat_messages_sender
                                   FOREIGN KEY (sender_id) REFERENCES users(id),
                               CONSTRAINT fk_chat_messages_sender_participant
                                   FOREIGN KEY (chat_room_id, sender_id)
                                       REFERENCES chat_room_participants(chat_room_id, user_id),
                               CONSTRAINT chk_chat_messages_body_not_blank
                                   CHECK (length(btrim(body)) > 0)
);

CREATE INDEX idx_chat_rooms_invite_post
    ON chat_rooms(invite_post_id);

CREATE INDEX idx_chat_room_participants_user_room
    ON chat_room_participants(user_id, chat_room_id);

CREATE INDEX idx_chat_messages_room_created_at
    ON chat_messages(chat_room_id, created_at, id);