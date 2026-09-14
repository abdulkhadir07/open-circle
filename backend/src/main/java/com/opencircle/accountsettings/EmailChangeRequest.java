package com.opencircle.accountsettings;

import com.opencircle.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "email_change_requests")
class EmailChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "new_email", nullable = false, length = 160)
    private String newEmail;

    @Column(name = "code_hash", nullable = false, length = 255)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected EmailChangeRequest() {
    }

    EmailChangeRequest(
            AppUser user,
            String newEmail,
            String codeHash,
            Instant createdAt,
            Instant expiresAt
    ) {
        if (user == null) {
            throw new IllegalArgumentException("Email-change user is required");
        }
        if (newEmail == null || newEmail.isBlank()) {
            throw new IllegalArgumentException("New email is required");
        }
        if (codeHash == null || codeHash.isBlank()) {
            throw new IllegalArgumentException("Email-change code hash is required");
        }
        if (createdAt == null || expiresAt == null || !expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("Email-change expiry must be after creation");
        }

        this.user = user;
        this.newEmail = newEmail.trim().toLowerCase(Locale.ROOT);
        this.codeHash = codeHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    boolean isActive(Instant now, int maxAttempts) {
        return usedAt == null
                && now != null
                && now.isBefore(expiresAt)
                && attemptCount < maxAttempts;
    }

    void recordFailedAttempt() {
        if (usedAt != null || attemptCount >= 10) {
            throw new IllegalStateException("Email-change request cannot record another attempt");
        }
        attemptCount++;
    }

    void markUsed(Instant at) {
        if (at == null || at.isBefore(createdAt)) {
            throw new IllegalArgumentException("Email-change use time is invalid");
        }
        if (usedAt == null) {
            usedAt = at;
        }
    }

    UUID getId() {
        return id;
    }

    AppUser getUser() {
        return user;
    }

    String getNewEmail() {
        return newEmail;
    }

    String getCodeHash() {
        return codeHash;
    }

    Instant getExpiresAt() {
        return expiresAt;
    }

    int getAttemptCount() {
        return attemptCount;
    }

    Instant getUsedAt() {
        return usedAt;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
