CREATE TABLE banters (
    id UUID PRIMARY KEY,
    author_id UUID NOT NULL,
    campus VARCHAR(160) NOT NULL,
    content VARCHAR(280) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_banters_author
        FOREIGN KEY (author_id)
            REFERENCES users(id)
            ON DELETE CASCADE,
    CONSTRAINT chk_banters_content_not_blank
        CHECK (BTRIM(content) <> ''),
    CONSTRAINT chk_banters_content_length
        CHECK (CHAR_LENGTH(content) <= 280)
);

CREATE INDEX idx_banters_campus_created
    ON banters (campus, created_at DESC, id DESC);

CREATE INDEX idx_banters_author
    ON banters (author_id);

CREATE TABLE banter_replies (
    id UUID PRIMARY KEY,
    banter_id UUID NOT NULL,
    author_id UUID NOT NULL,
    content VARCHAR(280) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_banter_replies_banter
        FOREIGN KEY (banter_id)
            REFERENCES banters(id)
            ON DELETE CASCADE,
    CONSTRAINT fk_banter_replies_author
        FOREIGN KEY (author_id)
            REFERENCES users(id)
            ON DELETE CASCADE,
    CONSTRAINT chk_banter_replies_content_not_blank
        CHECK (BTRIM(content) <> ''),
    CONSTRAINT chk_banter_replies_content_length
        CHECK (CHAR_LENGTH(content) <= 280)
);

CREATE INDEX idx_banter_replies_banter_created
    ON banter_replies (banter_id, created_at, id);

CREATE INDEX idx_banter_replies_author
    ON banter_replies (author_id);

CREATE TABLE banter_likes (
    id UUID PRIMARY KEY,
    banter_id UUID NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_banter_likes_banter
        FOREIGN KEY (banter_id)
            REFERENCES banters(id)
            ON DELETE CASCADE,
    CONSTRAINT fk_banter_likes_user
        FOREIGN KEY (user_id)
            REFERENCES users(id)
            ON DELETE CASCADE,
    CONSTRAINT uk_banter_likes_banter_user
        UNIQUE (banter_id, user_id)
);

CREATE INDEX idx_banter_likes_user
    ON banter_likes (user_id);
