CREATE TABLE user_profiles (
    user_id UUID PRIMARY KEY,
    display_name VARCHAR(80) NOT NULL,
    bio VARCHAR(300),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_user_profiles_user
        FOREIGN KEY (user_id)
            REFERENCES users(id)
            ON DELETE CASCADE,
    CONSTRAINT chk_user_profiles_display_name_not_blank
        CHECK (BTRIM(display_name) <> ''),
    CONSTRAINT chk_user_profiles_display_name_trimmed
        CHECK (display_name = BTRIM(display_name)),
    CONSTRAINT chk_user_profiles_bio_normalized
        CHECK (bio IS NULL OR (BTRIM(bio) <> '' AND bio = BTRIM(bio)))
);

INSERT INTO user_profiles (user_id, display_name, bio, created_at, updated_at)
SELECT
    id,
    COALESCE(NULLIF(BTRIM(first_name), ''), username),
    NULL,
    created_at,
    updated_at
FROM users;

CREATE TABLE user_profile_interests (
    user_id UUID NOT NULL,
    interest VARCHAR(30) NOT NULL,
    display_order SMALLINT NOT NULL,

    CONSTRAINT pk_user_profile_interests
        PRIMARY KEY (user_id, display_order),
    CONSTRAINT fk_user_profile_interests_profile
        FOREIGN KEY (user_id)
            REFERENCES user_profiles(user_id)
            ON DELETE CASCADE,
    CONSTRAINT chk_user_profile_interests_not_blank
        CHECK (BTRIM(interest) <> ''),
    CONSTRAINT chk_user_profile_interests_trimmed
        CHECK (interest = BTRIM(interest)),
    CONSTRAINT chk_user_profile_interests_display_order
        CHECK (display_order BETWEEN 0 AND 7)
);

CREATE UNIQUE INDEX uk_user_profile_interests_user_interest_ci
    ON user_profile_interests (user_id, LOWER(interest));
