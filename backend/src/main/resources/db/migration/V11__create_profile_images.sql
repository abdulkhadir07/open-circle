CREATE TABLE profile_images (
                                id UUID PRIMARY KEY,
                                user_id UUID NOT NULL,

                                original_filename VARCHAR(255) NOT NULL,
                                content_type VARCHAR(120) NOT NULL,
                                file_size_bytes BIGINT NOT NULL,
                                s3_bucket VARCHAR(255) NOT NULL,
                                s3_object_key VARCHAR(1024) NOT NULL UNIQUE,

                                created_at TIMESTAMPTZ NOT NULL,
                                updated_at TIMESTAMPTZ NOT NULL,

                                CONSTRAINT fk_profile_images_user
                                    FOREIGN KEY (user_id)
                                        REFERENCES users(id)
                                        ON DELETE CASCADE,

                                CONSTRAINT uk_profile_images_user
                                    UNIQUE (user_id),

                                CONSTRAINT chk_profile_images_original_filename_not_blank
                                    CHECK (length(btrim(original_filename)) > 0),

                                CONSTRAINT chk_profile_images_content_type
                                    CHECK (content_type IN ('image/jpeg', 'image/png', 'image/webp')),

                                CONSTRAINT chk_profile_images_file_size
                                    CHECK (file_size_bytes > 0 AND file_size_bytes <= 5242880)
);
