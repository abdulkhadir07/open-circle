ALTER TABLE invite_posts
    ADD COLUMN expiration_notified_at TIMESTAMPTZ;

-- Existing expirations predate this feature and must not generate retroactive notifications.
UPDATE invite_posts
SET expiration_notified_at = expires_at
WHERE expires_at <= CURRENT_TIMESTAMP;

ALTER TABLE invite_posts
    ADD CONSTRAINT chk_invite_posts_expiration_notification_time
        CHECK (
            expiration_notified_at IS NULL
            OR expiration_notified_at >= expires_at
        );

CREATE INDEX idx_invite_posts_pending_expiration_notification
    ON invite_posts (expires_at, id)
    WHERE expiration_notified_at IS NULL;

ALTER TABLE notifications
    DROP CONSTRAINT chk_notifications_type;

ALTER TABLE notifications
    ADD CONSTRAINT chk_notifications_type
        CHECK (type IN (
            'ENGAGEMENT_REQUESTED',
            'ENGAGEMENT_ACCEPTED',
            'ENGAGEMENT_DECLINED',
            'ENGAGEMENT_HELD',
            'ENGAGEMENT_WITHDRAWN',
            'INVITE_POST_EXPIRED',
            'ENGAGEMENT_REQUEST_EXPIRED'
        ));
