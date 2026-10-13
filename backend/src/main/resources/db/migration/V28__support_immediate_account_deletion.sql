-- Account deletion removes user-owned data while preserving records that belong to other users.
ALTER TABLE chat_rooms
    DROP CONSTRAINT fk_chat_rooms_invite_post,
    ADD CONSTRAINT fk_chat_rooms_invite_post
        FOREIGN KEY (invite_post_id) REFERENCES invite_posts(id) ON DELETE CASCADE;

ALTER TABLE chat_room_participants
    DROP CONSTRAINT fk_chat_room_participants_user,
    ADD CONSTRAINT fk_chat_room_participants_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE chat_messages
    DROP CONSTRAINT fk_chat_messages_sender_participant,
    DROP CONSTRAINT fk_chat_messages_sender,
    ADD CONSTRAINT fk_chat_messages_sender
        FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_chat_messages_sender_participant
        FOREIGN KEY (chat_room_id, sender_id)
            REFERENCES chat_room_participants(chat_room_id, user_id) ON DELETE CASCADE;

ALTER TABLE chat_attachments
    DROP CONSTRAINT fk_chat_attachments_uploader,
    ADD CONSTRAINT fk_chat_attachments_uploader
        FOREIGN KEY (uploader_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE invite_post_images
    DROP CONSTRAINT fk_invite_post_images_uploader,
    ADD CONSTRAINT fk_invite_post_images_uploader
        FOREIGN KEY (uploader_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE annual_awards
    DROP CONSTRAINT fk_annual_awards_winner,
    ADD CONSTRAINT fk_annual_awards_winner
        FOREIGN KEY (winner_user_id) REFERENCES users(id) ON DELETE CASCADE;

-- Preserve shared chat state without retaining which deleted user performed the action.
ALTER TABLE chat_rooms
    DROP CONSTRAINT fk_chat_rooms_saved_by,
    DROP CONSTRAINT chk_chat_rooms_saved_state,
    ADD CONSTRAINT fk_chat_rooms_saved_by
        FOREIGN KEY (saved_by_id) REFERENCES users(id) ON DELETE SET NULL,
    ADD CONSTRAINT chk_chat_rooms_saved_state
        CHECK (
            (saved_at IS NULL AND saved_by_id IS NULL)
            OR saved_at IS NOT NULL
        );

ALTER TABLE chat_room_participants
    DROP CONSTRAINT fk_chat_room_participants_removed_by,
    DROP CONSTRAINT chk_chat_room_participants_removed_state,
    ADD CONSTRAINT fk_chat_room_participants_removed_by
        FOREIGN KEY (removed_by_id) REFERENCES users(id) ON DELETE SET NULL,
    ADD CONSTRAINT chk_chat_room_participants_removed_state
        CHECK (
            (removed_at IS NULL AND removed_by_id IS NULL)
            OR removed_at IS NOT NULL
        );
