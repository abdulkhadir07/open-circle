package com.opencircle.invitepost;

import com.opencircle.user.AppUser;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Entity
@Table(name = "invite_posts")
public class InvitePost {

    private static final int EXPIRATION_HOURS = 24;

    static final int MAX_TAGS = 5;
    static final int MAX_TAG_LENGTH = 30;
    private static final Pattern TAG_PATTERN = Pattern.compile("[\\p{L}\\p{N}][\\p{L}\\p{N}_-]*");

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

    @Column(nullable = false, length = 160)
    private String campus;

    @Column(length = 80)
    private String city;

    @Column(name = "state_region", length = 80)
    private String stateRegion;

    @Column(length = 80)
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InvitePostStatus status = InvitePostStatus.ACTIVE;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "expiration_notified_at", insertable = false, updatable = false)
    private Instant expirationNotifiedAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "invite_post_tags",
            joinColumns = @JoinColumn(name = "post_id"),
            foreignKey = @ForeignKey(name = "fk_invite_post_tags_post")
    )
    @OrderBy("displayOrder ASC")
    private List<InvitePostTag> tags = new ArrayList<>();

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
        this(poster, content, inviteType, totalCapacity, locationScope, city, stateRegion, country, createdAt, List.of());
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
            Instant createdAt,
            List<String> tags
    ) {
        validateCore(poster, content, inviteType, totalCapacity, createdAt);

        if (locationScope == null) {
            throw new IllegalArgumentException("Location scope is required");
        }

        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("City is required");
        }

        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Country is required");
        }

        if (locationScope == LocationScope.STATE_REGION && (stateRegion == null || stateRegion.isBlank())) {
            throw new IllegalArgumentException("State/region is required for state-region scoped posts");
        }

        this.poster = poster;
        this.campus = poster.getCampus();
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

        applyTags(tags);
    }

    private static void validateCore(
            AppUser poster,
            String content,
            InviteType inviteType,
            int totalCapacity,
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

        if (createdAt == null) {
            throw new IllegalArgumentException("Created time is required");
        }

        if (inviteType == InviteType.SINGLE && totalCapacity != 1) {
            throw new IllegalArgumentException("Single invites must have a capacity of 1");
        }

        if (inviteType == InviteType.GROUP && totalCapacity < 2) {
            throw new IllegalArgumentException("Group invites must have a capacity of at least 2");
        }
    }

    private void applyTags(List<String> tags) {
        List<String> normalizedTags = normalizeTags(tags);
        for (short index = 0; index < normalizedTags.size(); index++) {
            this.tags.add(new InvitePostTag(normalizedTags.get(index), index));
        }
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

    public String getCampus() {
        return campus;
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

    public Instant getExpirationNotifiedAt() {
        return expirationNotifiedAt;
    }

    public List<String> getTags() {
        return tags.stream()
                .map(InvitePostTag::getValue)
                .toList();
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

    // Tags are stored lowercase without the leading '#'; blanks are dropped and duplicates collapse.
    private static List<String> normalizeTags(List<String> values) {
        if (values == null) {
            return List.of();
        }

        Set<String> normalized = new LinkedHashSet<>();

        for (String value : values) {
            if (value == null) {
                continue;
            }

            String tag = value.trim();
            while (tag.startsWith("#")) {
                tag = tag.substring(1);
            }
            tag = tag.trim().toLowerCase(Locale.ROOT);

            if (tag.isEmpty()) {
                continue;
            }

            if (tag.length() > MAX_TAG_LENGTH) {
                throw new IllegalArgumentException("Tags must not exceed 30 characters");
            }

            if (!TAG_PATTERN.matcher(tag).matches()) {
                throw new IllegalArgumentException("Tags can only contain letters, numbers, hyphens and underscores");
            }

            normalized.add(tag);
        }

        if (normalized.size() > MAX_TAGS) {
            throw new IllegalArgumentException("A post can have at most 5 tags");
        }

        return List.copyOf(normalized);
    }
}
