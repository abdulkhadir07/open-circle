package com.opencircle.accountsettings;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class EmailClaimLock {

    private final JdbcTemplate jdbcTemplate;

    EmailClaimLock(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    void acquire(String normalizedEmail) {
        jdbcTemplate.query(
                "SELECT pg_advisory_xact_lock(hashtextextended(?, 0))",
                resultSet -> {
                    // The query blocks until the transaction owns the lock; no result data is needed.
                },
                normalizedEmail
        );
    }
}
