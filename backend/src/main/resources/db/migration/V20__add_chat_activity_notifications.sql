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
            'CHAT_ACTIVITY'
        ));

ALTER TABLE notifications
    DROP CONSTRAINT chk_notifications_resource_type;

ALTER TABLE notifications
    ADD CONSTRAINT chk_notifications_resource_type
        CHECK (resource_type IN ('ENGAGEMENT_REQUEST', 'INVITE_POST', 'CHAT_ROOM'));

ALTER TABLE notifications
    DROP CONSTRAINT chk_notifications_context_type;

ALTER TABLE notifications
    ADD CONSTRAINT chk_notifications_context_type
        CHECK (context_type IS NULL OR context_type IN ('ENGAGEMENT_REQUEST', 'INVITE_POST', 'CHAT_ROOM'));
