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
            'ENGAGEMENT_REQUEST_EXPIRED',
            'CHAT_ACTIVITY',
            'RATING_REQUIRED',
            'RATING_REVEALED'
        ));
