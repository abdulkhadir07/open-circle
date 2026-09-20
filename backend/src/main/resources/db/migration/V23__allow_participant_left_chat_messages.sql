ALTER TABLE chat_messages
    DROP CONSTRAINT chk_chat_messages_type;

ALTER TABLE chat_messages
    ADD CONSTRAINT chk_chat_messages_type
        CHECK (type IN ('TEXT', 'ATTACHMENT', 'PARTICIPANT_LEFT'));

ALTER TABLE chat_messages
    DROP CONSTRAINT chk_chat_messages_body_valid_for_type;

ALTER TABLE chat_messages
    ADD CONSTRAINT chk_chat_messages_body_valid_for_type
        CHECK (
            (type = 'TEXT' AND body IS NOT NULL AND length(btrim(body)) > 0)
                OR
            (type = 'ATTACHMENT' AND (body IS NULL OR length(btrim(body)) > 0))
                OR
            (type = 'PARTICIPANT_LEFT' AND body IS NULL)
            );
