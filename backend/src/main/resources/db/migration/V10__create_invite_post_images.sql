CREATE TABLE invite_post_images (
                                    id UUID PRIMARY KEY,
                                    invite_post_id UUID NOT NULL,
                                    uploader_id UUID NOT NULL,

                                    original_filename VARCHAR(255) NOT NULL,
                                    content_type VARCHAR(120) NOT NULL,
                                    file_size_bytes BIGINT NOT NULL,
                                    s3_bucket VARCHAR(255) NOT NULL,
                                    s3_object_key VARCHAR(1024) NOT NULL UNIQUE,

                                    display_order INTEGER NOT NULL,
                                    created_at TIMESTAMPTZ NOT NULL,

                                    CONSTRAINT fk_invite_post_images_post
                                        FOREIGN KEY (invite_post_id)
                                            REFERENCES invite_posts(id)
                                            ON DELETE CASCADE,

                                    CONSTRAINT fk_invite_post_images_uploader
                                        FOREIGN KEY (uploader_id)
                                            REFERENCES users(id)
                                            ON DELETE RESTRICT,

                                    CONSTRAINT uk_invite_post_images_post_display_order
                                        UNIQUE (invite_post_id, display_order),

                                    CONSTRAINT chk_invite_post_images_original_filename_not_blank
                                        CHECK (length(btrim(original_filename)) > 0),

                                    CONSTRAINT chk_invite_post_images_content_type
                                        CHECK (content_type IN ('image/jpeg', 'image/png', 'image/webp')),

                                    CONSTRAINT chk_invite_post_images_file_size
                                        CHECK (file_size_bytes > 0 AND file_size_bytes <= 5242880),

                                    CONSTRAINT chk_invite_post_images_display_order
                                        CHECK (display_order BETWEEN 1 AND 4)
);
