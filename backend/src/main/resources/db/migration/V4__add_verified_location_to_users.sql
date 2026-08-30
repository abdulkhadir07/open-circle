ALTER TABLE users
    ALTER COLUMN state_region DROP NOT NULL,
    ADD COLUMN verified_city VARCHAR(80),
    ADD COLUMN verified_state_region VARCHAR(80),
    ADD COLUMN verified_country VARCHAR(80),
    ADD COLUMN location_verified_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN location_source VARCHAR(30);

ALTER TABLE users
    ADD CONSTRAINT chk_users_location_source
        CHECK (location_source IS NULL OR location_source IN ('DEVICE')),
    ADD CONSTRAINT chk_users_verified_location_complete
        CHECK (
            (
                verified_city IS NULL
                AND verified_country IS NULL
                AND location_verified_at IS NULL
                AND location_source IS NULL
            )
            OR
            (
                verified_city IS NOT NULL
                AND verified_country IS NOT NULL
                AND location_verified_at IS NOT NULL
                AND location_source IS NOT NULL
            )
        );

CREATE INDEX idx_users_verified_country
    ON users (verified_country);

CREATE INDEX idx_users_verified_country_state_region
    ON users (verified_country, verified_state_region);

CREATE INDEX idx_users_verified_country_city
    ON users (verified_country, verified_city);