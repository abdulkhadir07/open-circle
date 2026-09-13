package com.opencircle.session;

import com.opencircle.user.AppUser;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_sessions")
class AuthSession {

    static final int MAX_USER_AGENT_LENGTH = 512;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "user_agent", length = MAX_USER_AGENT_LENGTH)
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_used_at", nullable = false)
    private Instant lastUsedAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "revocation_reason", length = 40)
    private SessionRevocationReason revocationReason;

    protected AuthSession() {
    }

    AuthSession(AppUser user, String userAgent, Instant createdAt, Instant expiresAt) {
        if (user == null) {
            throw new IllegalArgumentException("Session user is required");
        }
        if (createdAt == null || expiresAt == null || !expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("Session expiry must be after creation");
        }

        this.user = user;
        this.userAgent = normalizeUserAgent(userAgent);
        this.createdAt = createdAt;
        this.lastUsedAt = createdAt;
        this.expiresAt = expiresAt;
    }

    void recordUse(Instant usedAt) {
        if (usedAt == null || usedAt.isBefore(lastUsedAt) || usedAt.isAfter(expiresAt)) {
            throw new IllegalArgumentException("Session use time is outside the session lifetime");
        }
        if (revokedAt != null) {
            throw new IllegalStateException("Revoked session cannot be used");
        }

        lastUsedAt = usedAt;
    }

    void revoke(Instant at, SessionRevocationReason reason) {
        if (at == null || reason == null) {
            throw new IllegalArgumentException("Session revocation time and reason are required");
        }
        if (revokedAt != null) {
            return;
        }

        revokedAt = at;
        revocationReason = reason;
    }

    boolean isActive(Instant now) {
        return revokedAt == null && now != null && now.isBefore(expiresAt);
    }

    UUID getId() {
        return id;
    }

    AppUser getUser() {
        return user;
    }

    String getUserAgent() {
        return userAgent;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getLastUsedAt() {
        return lastUsedAt;
    }

    Instant getExpiresAt() {
        return expiresAt;
    }

    Instant getRevokedAt() {
        return revokedAt;
    }

    SessionRevocationReason getRevocationReason() {
        return revocationReason;
    }

    private String normalizeUserAgent(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();
        return normalized.length() <= MAX_USER_AGENT_LENGTH
                ? normalized
                : normalized.substring(0, MAX_USER_AGENT_LENGTH);
    }
}
