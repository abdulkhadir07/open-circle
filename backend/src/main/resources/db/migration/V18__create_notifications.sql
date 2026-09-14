CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    recipient_user_id UUID NOT NULL,
    actor_user_id UUID,
    type VARCHAR(50) NOT NULL,
    resource_type VARCHAR(40) NOT NULL,
    resource_id UUID NOT NULL,
    context_type VARCHAR(40),
    context_id UUID,
    occurrence_count INTEGER NOT NULL DEFAULT 1,
    occurred_at TIMESTAMPTZ NOT NULL,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_notifications_recipient
        FOREIGN KEY (recipient_user_id)
            REFERENCES users(id)
            ON DELETE CASCADE,
    CONSTRAINT fk_notifications_actor
        FOREIGN KEY (actor_user_id)
            REFERENCES users(id)
            ON DELETE SET NULL,
    CONSTRAINT uk_notifications_recipient_type_resource
        UNIQUE (recipient_user_id, type, resource_type, resource_id),
    CONSTRAINT chk_notifications_type
        CHECK (type IN (
            'ENGAGEMENT_REQUESTED',
            'ENGAGEMENT_ACCEPTED',
            'ENGAGEMENT_DECLINED',
            'ENGAGEMENT_HELD',
            'ENGAGEMENT_WITHDRAWN'
        )),
    CONSTRAINT chk_notifications_resource_type
        CHECK (resource_type IN ('ENGAGEMENT_REQUEST', 'INVITE_POST')),
    CONSTRAINT chk_notifications_context_pair
        CHECK (
            (context_type IS NULL AND context_id IS NULL)
            OR (context_type IS NOT NULL AND context_id IS NOT NULL)
        ),
    CONSTRAINT chk_notifications_context_type
        CHECK (context_type IS NULL OR context_type IN ('ENGAGEMENT_REQUEST', 'INVITE_POST')),
    CONSTRAINT chk_notifications_occurrence_count
        CHECK (occurrence_count >= 1),
    CONSTRAINT chk_notifications_timestamps
        CHECK (
            created_at >= occurred_at
            AND (read_at IS NULL OR read_at >= created_at)
        )
);

CREATE INDEX idx_notifications_recipient_occurred
    ON notifications (recipient_user_id, occurred_at DESC, id DESC);

CREATE INDEX idx_notifications_recipient_unread
    ON notifications (recipient_user_id, occurred_at DESC, id DESC)
    WHERE read_at IS NULL;

CREATE INDEX idx_notifications_retention
    ON notifications (occurred_at);
