package com.opencircle.invitepost;

import com.opencircle.user.AppUser;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invite_posts")
public class InvitePost {

    private static final int EXPIRATION_HOURS = 24;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "poster_id", nullable = false)
    private AppUser poster;

    @Column(nullable = false, length = 500)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "invite_type", nullable = false, length = 30)
    private InviteType inviteType;

    @Column(name = "total_capacity", nullable = false)
    private int totalCapacity;

    @Column(name = "accepted_count", nullable = false)
    private int acceptedCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_scope", nullable = false, length = 30)
    private LocationScope locationScope;

    @Column(nullable = false, length = 80)
    private String city;

    @Column(name = "state_region", length = 80)
    private String stateRegion;

    @Column(nullable = false, length = 80)
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InvitePostStatus status = InvitePostStatus.ACTIVE;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InvitePost() {
    }

    public InvitePost(
            AppUser poster,
            String content,
            InviteType inviteType,
            int totalCapacity,
            LocationScope locationScope,
            String city,
            String stateRegion,
            String country,
            Instant createdAt
    ) {
        if (poster == null) {
            throw new IllegalArgumentException("Poster is required");
        }

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Content is required");
        }

        if (inviteType == null) {
            throw new IllegalArgumentException("Invite type is required");
        }

        if (locationScope == null) {
            throw new IllegalArgumentException("Location scope is required");
        }

        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("City is required");
        }

        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Country is required");
        }

        if (createdAt == null) {
            throw new IllegalArgumentException("Created time is required");
        }

        if (inviteType == InviteType.SINGLE && totalCapacity != 1) {
            throw new IllegalArgumentException("Single invites must have a capacity of 1");
        }

        if (inviteType == InviteType.GROUP && totalCapacity < 2) {
            throw new IllegalArgumentException("Group invites must have a capacity of at least 2");
        }

        if (locationScope == LocationScope.STATE_REGION && (stateRegion == null || stateRegion.isBlank())) {
            throw new IllegalArgumentException("State/region is required for state-region scoped posts");
        }

        this.poster = poster;
        this.content = content.trim();
        this.inviteType = inviteType;
        this.totalCapacity = totalCapacity;
        this.locationScope = locationScope;
        this.city = city.trim();
        this.stateRegion = stateRegion == null || stateRegion.isBlank() ? null : stateRegion.trim();
        this.country = country.trim();
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.expiresAt = createdAt.plusSeconds(EXPIRATION_HOURS * 60L * 60L);
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            Instant now = Instant.now();
            createdAt = now;
            updatedAt = now;
            expiresAt = now.plusSeconds(EXPIRATION_HOURS * 60L * 60L);
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public AppUser getPoster() {
        return poster;
    }

    public String getContent() {
        return content;
    }

    public InviteType getInviteType() {
        return inviteType;
    }

    public int getTotalCapacity() {
        return totalCapacity;
    }

    public int getAcceptedCount() {
        return acceptedCount;
    }

    public int getInvitesLeft() {
        return totalCapacity - acceptedCount;
    }

    public LocationScope getLocationScope() {
        return locationScope;
    }

    public String getCity() {
        return city;
    }

    public String getStateRegion() {
        return stateRegion;
    }

    public String getCountry() {
        return country;
    }

    public InvitePostStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public boolean isOpen(Instant now) {
        return status == InvitePostStatus.ACTIVE && !isExpired(now) && acceptedCount < totalCapacity;
    }

    public void close() {
        status = InvitePostStatus.CLOSED;
    }

    public void recordAcceptedEngagement() {
        if (acceptedCount >= totalCapacity) {
            throw new IllegalStateException("Invite post is already full");
        }

        acceptedCount++;
    }
}