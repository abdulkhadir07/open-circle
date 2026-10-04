package com.opencircle.banter;

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
import java.util.UUID;

// One like per user per banter; the unique constraint is enforced in the database.
@Entity
@Table(name = "banter_likes")
class BanterLike {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "banter_id", nullable = false)
    private Banter banter;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected BanterLike() {
    }

    BanterLike(Banter banter, AppUser user, Instant createdAt) {
        this.banter = banter;
        this.user = user;
        this.createdAt = createdAt;
    }
}
