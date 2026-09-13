package com.opencircle.session;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "session_refresh_tokens",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_session_refresh_tokens_hash",
                columnNames = "token_hash"
        )
)
class SessionRefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private AuthSession session;

    @Column(name = "token_hash", nullable = false, length = 64, updatable = false)
    private String tokenHash;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    protected SessionRefreshToken() {
    }

    SessionRefreshToken(AuthSession session, String tokenHash, Instant issuedAt, Instant expiresAt) {
        if (session == null) {
            throw new IllegalArgumentException("Refresh-token session is required");
        }
        if (tokenHash == null || !tokenHash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Refresh-token hash must be a SHA-256 hex value");
        }
        if (issuedAt == null || expiresAt == null || !expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("Refresh-token expiry must be after issue time");
        }

        this.session = session;
        this.tokenHash = tokenHash;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    void markUsed(Instant at) {
        if (usedAt != null) {
            throw new IllegalStateException("Refresh token has already been used");
        }
        if (at == null || at.isBefore(issuedAt) || at.isAfter(expiresAt)) {
            throw new IllegalArgumentException("Refresh-token use time is outside its lifetime");
        }

        usedAt = at;
    }

    boolean isActive(Instant now) {
        return usedAt == null && now != null && now.isBefore(expiresAt);
    }

    UUID getId() {
        return id;
    }

    AuthSession getSession() {
        return session;
    }

    String getTokenHash() {
        return tokenHash;
    }

    Instant getIssuedAt() {
        return issuedAt;
    }

    Instant getExpiresAt() {
        return expiresAt;
    }

    Instant getUsedAt() {
        return usedAt;
    }
}
