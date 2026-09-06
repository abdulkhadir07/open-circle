-- Existing chat messages are text messages during migration backfill.
ALTER TABLE chat_messages
    ADD COLUMN type VARCHAR(30) NOT NULL DEFAULT 'TEXT';

ALTER TABLE chat_messages
    ADD CONSTRAINT chk_chat_messages_type
        CHECK (type IN ('TEXT', 'ATTACHMENT'));

-- Attachment messages use body as an optional caption. Text messages still require body.
ALTER TABLE chat_messages
    ALTER COLUMN body DROP NOT NULL;

ALTER TABLE chat_messages
DROP CONSTRAINT chk_chat_messages_body_not_blank;

ALTER TABLE chat_messages
    ADD CONSTRAINT chk_chat_messages_body_valid_for_type
        CHECK (
            (type = 'TEXT' AND body IS NOT NULL AND length(btrim(body)) > 0)
                OR
            (type = 'ATTACHMENT' AND (body IS NULL OR length(btrim(body)) > 0))
            );

CREATE TABLE chat_attachments (
                                  id UUID PRIMARY KEY,
                                  chat_message_id UUID NOT NULL UNIQUE,
                                  chat_room_id UUID NOT NULL,
                                  uploader_id UUID NOT NULL,

                                  original_filename VARCHAR(255) NOT NULL,
                                  content_type VARCHAR(120) NOT NULL,
                                  file_size_bytes BIGINT NOT NULL,
                                  s3_bucket VARCHAR(255) NOT NULL,
                                  s3_object_key VARCHAR(1024) NOT NULL UNIQUE,

                                  created_at TIMESTAMPTZ NOT NULL,

                                  CONSTRAINT fk_chat_attachments_message
                                      FOREIGN KEY (chat_message_id)
                                          REFERENCES chat_messages(id)
                                          ON DELETE CASCADE,

                                  CONSTRAINT fk_chat_attachments_room
                                      FOREIGN KEY (chat_room_id)
                                          REFERENCES chat_rooms(id)
                                          ON DELETE CASCADE,

                                  CONSTRAINT fk_chat_attachments_uploader
                                      FOREIGN KEY (uploader_id)
                                          REFERENCES users(id)
                                          ON DELETE RESTRICT,

                                  CONSTRAINT chk_chat_attachments_original_filename_not_blank
                                      CHECK (length(btrim(original_filename)) > 0),

                                  CONSTRAINT chk_chat_attachments_content_type_not_blank
                                      CHECK (length(btrim(content_type)) > 0),

                                  CONSTRAINT chk_chat_attachments_file_size_positive
                                      CHECK (file_size_bytes > 0)
);

CREATE INDEX idx_chat_attachments_room_created_at
    ON chat_attachments(chat_room_id, created_at DESC);

CREATE INDEX idx_chat_attachments_uploader_created_at
    ON chat_attachments(uploader_id, created_at DESC);

-- New app code writes message type explicitly, so missing type values fail loudly.
ALTER TABLE chat_messages
    ALTER COLUMN type DROP DEFAULT;