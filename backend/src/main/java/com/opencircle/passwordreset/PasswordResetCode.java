package com.opencircle.passwordreset;

import com.opencircle.user.AppUser;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "password_reset_codes")
class PasswordResetCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PasswordResetCode() {
    }

    PasswordResetCode(AppUser user, String codeHash, Instant expiresAt) {
        this.user = user;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    UUID getId() {
        return id;
    }

    AppUser getUser() {
        return user;
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

    boolean isUsed() {
        return usedAt != null;
    }

    boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    // Reset codes should stop being usable after too many failed guesses.
    boolean isActive(Instant now, int maxAttempts) {
        return !isUsed()
                && !isExpired(now)
                && attemptCount < maxAttempts;
    }

    void recordFailedAttempt() {
        attemptCount++;
    }

    void markUsed(Instant usedAt) {
        if (usedAt == null) {
            throw new IllegalArgumentException("Used time is required");
        }

        this.usedAt = usedAt;
    }

}
