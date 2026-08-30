CREATE TABLE invite_posts (
                              id UUID PRIMARY KEY,
                              poster_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

                              content VARCHAR(500) NOT NULL,

                              invite_type VARCHAR(30) NOT NULL,
                              total_capacity INTEGER NOT NULL,
                              accepted_count INTEGER NOT NULL DEFAULT 0,

                              location_scope VARCHAR(30) NOT NULL,
                              city VARCHAR(80) NOT NULL,
                              state_region VARCHAR(80),
                              country VARCHAR(80) NOT NULL,

                              status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
                              expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                              created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                              updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

                              CONSTRAINT chk_invite_posts_invite_type
                                  CHECK (invite_type IN ('SINGLE', 'GROUP')),

                              CONSTRAINT chk_invite_posts_location_scope
                                  CHECK (location_scope IN ('CITY', 'STATE_REGION', 'COUNTRY', 'GLOBAL')),

                              CONSTRAINT chk_invite_posts_status
                                  CHECK (status IN ('ACTIVE', 'CLOSED')),

                              CONSTRAINT chk_invite_posts_content_not_blank
                                  CHECK (length(trim(content)) > 0),

                              CONSTRAINT chk_invite_posts_total_capacity_positive
                                  CHECK (total_capacity > 0),

                              CONSTRAINT chk_invite_posts_accepted_count_valid
                                  CHECK (accepted_count >= 0 AND accepted_count <= total_capacity),

                              CONSTRAINT chk_invite_posts_single_capacity
                                  CHECK (
                                      (invite_type = 'SINGLE' AND total_capacity = 1)
                                          OR invite_type = 'GROUP'
                                      ),

                              CONSTRAINT chk_invite_posts_state_scope_has_state
                                  CHECK (
                                      location_scope <> 'STATE_REGION'
                                          OR state_region IS NOT NULL
                                      )
);

CREATE INDEX idx_invite_posts_poster_id
    ON invite_posts (poster_id);

CREATE INDEX idx_invite_posts_active_country
    ON invite_posts (status, expires_at, country, location_scope);

CREATE INDEX idx_invite_posts_active_state_region
    ON invite_posts (status, expires_at, country, state_region, location_scope);

CREATE INDEX idx_invite_posts_active_city
    ON invite_posts (status, expires_at, country, city, location_scope);

CREATE INDEX idx_invite_posts_active_global
    ON invite_posts (status, expires_at, location_scope);