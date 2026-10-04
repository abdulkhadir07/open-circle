-- A campus is the registrable domain of a user's email (student.sfsu.edu -> sfsu.edu).
ALTER TABLE users ADD COLUMN campus VARCHAR(160);

UPDATE users
SET campus = regexp_replace(lower(split_part(email, '@', 2)), '^.*\.([^.]+\.[^.]+)$', '\1');

ALTER TABLE users ALTER COLUMN campus SET NOT NULL;

CREATE INDEX idx_users_campus ON users (campus);

-- Invite posts belong to their poster's campus.
ALTER TABLE invite_posts ADD COLUMN campus VARCHAR(160);

UPDATE invite_posts
SET campus = users.campus
FROM users
WHERE users.id = invite_posts.poster_id;

ALTER TABLE invite_posts ALTER COLUMN campus SET NOT NULL;

CREATE INDEX idx_invite_posts_active_campus
    ON invite_posts (status, expires_at, campus);

-- Campus accounts and posts no longer need a street-level location; the old columns stay for
-- when location-scoped feeds come back, but are optional.
ALTER TABLE users ALTER COLUMN city DROP NOT NULL;
ALTER TABLE users ALTER COLUMN country DROP NOT NULL;
ALTER TABLE invite_posts ALTER COLUMN city DROP NOT NULL;
ALTER TABLE invite_posts ALTER COLUMN country DROP NOT NULL;

ALTER TABLE invite_posts DROP CONSTRAINT chk_invite_posts_location_scope;
ALTER TABLE invite_posts
    ADD CONSTRAINT chk_invite_posts_location_scope
        CHECK (location_scope IN ('CAMPUS', 'CITY', 'STATE_REGION', 'COUNTRY', 'GLOBAL'));
