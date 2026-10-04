CREATE TABLE invite_post_tags (
    post_id UUID NOT NULL,
    tag VARCHAR(30) NOT NULL,
    display_order SMALLINT NOT NULL,

    CONSTRAINT pk_invite_post_tags
        PRIMARY KEY (post_id, display_order),
    CONSTRAINT fk_invite_post_tags_post
        FOREIGN KEY (post_id)
            REFERENCES invite_posts(id)
            ON DELETE CASCADE,
    CONSTRAINT chk_invite_post_tags_not_blank
        CHECK (BTRIM(tag) <> ''),
    CONSTRAINT chk_invite_post_tags_normalized
        CHECK (tag = LOWER(BTRIM(tag))),
    CONSTRAINT chk_invite_post_tags_display_order
        CHECK (display_order BETWEEN 0 AND 4)
);

CREATE UNIQUE INDEX uk_invite_post_tags_post_tag
    ON invite_post_tags (post_id, tag);
