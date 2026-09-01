CREATE TABLE engagement_requests (
                                     id UUID PRIMARY KEY,
                                     invite_post_id UUID NOT NULL REFERENCES invite_posts(id) ON DELETE CASCADE,
                                     requester_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

                                     status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                                     expires_at TIMESTAMP WITH TIME ZONE NOT NULL,

                                     responded_at TIMESTAMP WITH TIME ZONE,
                                     withdrawn_at TIMESTAMP WITH TIME ZONE,

                                     created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                     updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

                                     CONSTRAINT uk_engagement_requests_post_requester
                                         UNIQUE (invite_post_id, requester_id),

                                     CONSTRAINT chk_engagement_requests_status
                                         CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'HELD', 'WITHDRAWN')),

                                     CONSTRAINT chk_engagement_requests_expires_after_created
                                         CHECK (expires_at > created_at),

                                     CONSTRAINT chk_engagement_requests_response_timestamp
                                         CHECK (
                                             (status IN ('ACCEPTED', 'DECLINED', 'HELD') AND responded_at IS NOT NULL)
                                                 OR (status NOT IN ('ACCEPTED', 'DECLINED', 'HELD') AND responded_at IS NULL)
                                             ),

                                     CONSTRAINT chk_engagement_requests_withdrawn_timestamp
                                         CHECK (
                                             (status = 'WITHDRAWN' AND withdrawn_at IS NOT NULL)
                                                 OR (status <> 'WITHDRAWN' AND withdrawn_at IS NULL)
                                             )
);

CREATE INDEX idx_engagement_requests_invite_post_status
    ON engagement_requests (invite_post_id, status, created_at DESC);

CREATE INDEX idx_engagement_requests_requester_status
    ON engagement_requests (requester_id, status, created_at DESC);

CREATE INDEX idx_engagement_requests_actionable_expiration
    ON engagement_requests (status, expires_at)
    WHERE status IN ('PENDING', 'HELD');