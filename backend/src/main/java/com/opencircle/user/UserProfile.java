package com.opencircle.user;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "user_profiles")
class UserProfile {

    static final int MAX_DISPLAY_NAME_LENGTH = 80;
    static final int MAX_BIO_LENGTH = 300;
    static final int MAX_INTERESTS = 8;
    static final int MAX_INTEREST_LENGTH = 30;

    @Id
    @Column(name = "user_id", updatable = false)
    private UUID userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_user_profiles_user")
    )
    private AppUser user;

    @Column(name = "display_name", nullable = false, length = MAX_DISPLAY_NAME_LENGTH)
    private String displayName;

    @Column(length = MAX_BIO_LENGTH)
    private String bio;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "user_profile_interests",
            joinColumns = @JoinColumn(name = "user_id"),
            foreignKey = @ForeignKey(name = "fk_user_profile_interests_profile")
    )
    @OrderBy("displayOrder ASC")
    private List<UserProfileInterest> interests = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserProfile() {
    }

    UserProfile(AppUser user, Instant createdAt) {
        if (user == null) {
            throw new IllegalArgumentException("Profile user is required");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("Profile creation time is required");
        }

        this.user = user;
        this.displayName = normalizeDisplayName(user.getFirstName());
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    void replace(String displayName, String bio, List<String> interests, Instant updatedAt) {
        if (updatedAt == null) {
            throw new IllegalArgumentException("Profile update time is required");
        }

        String normalizedDisplayName = normalizeDisplayName(displayName);
        String normalizedBio = normalizeBio(bio);
        List<String> normalizedInterests = normalizeInterests(interests);

        this.displayName = normalizedDisplayName;
        this.bio = normalizedBio;
        this.interests.clear();
        for (short index = 0; index < normalizedInterests.size(); index++) {
            this.interests.add(new UserProfileInterest(normalizedInterests.get(index), index));
        }
        this.updatedAt = updatedAt;
    }

    UUID getUserId() {
        return userId;
    }

    AppUser getUser() {
        return user;
    }

    String getDisplayName() {
        return displayName;
    }

    String getBio() {
        return bio;
    }

    List<String> getInterests() {
        return interests.stream()
                .map(UserProfileInterest::getValue)
                .toList();
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }

    private String normalizeDisplayName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Display name is required");
        }

        String normalized = value.trim();
        if (normalized.length() > MAX_DISPLAY_NAME_LENGTH) {
            throw new IllegalArgumentException("Display name must not exceed 80 characters");
        }
        return normalized;
    }

    private String normalizeBio(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();
        if (normalized.length() > MAX_BIO_LENGTH) {
            throw new IllegalArgumentException("Bio must not exceed 300 characters");
        }
        return normalized;
    }

    private List<String> normalizeInterests(List<String> values) {
        if (values == null) {
            throw new IllegalArgumentException("Interests are required; use an empty list for none");
        }
        if (values.size() > MAX_INTERESTS) {
            throw new IllegalArgumentException("A profile can have at most 8 interests");
        }

        List<String> normalized = new ArrayList<>(values.size());
        Set<String> uniqueValues = new HashSet<>();

        for (String value : values) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Interests cannot be blank");
            }

            String normalizedValue = value.trim();
            if (normalizedValue.length() > MAX_INTEREST_LENGTH) {
                throw new IllegalArgumentException("Interests must not exceed 30 characters");
            }
            if (!uniqueValues.add(normalizedValue.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Interests must be unique ignoring case");
            }

            normalized.add(normalizedValue);
        }

        return normalized;
    }
}
