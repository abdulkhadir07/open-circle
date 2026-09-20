package com.opencircle.user;

import jakarta.persistence.*;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_users_username", columnNames = "username"),
                @UniqueConstraint(name = "uk_users_phone_number", columnNames = "phone_number")
        }
)
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 40)
    private String username;

    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;

    @Column(nullable = false, length = 160)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "phone_number", nullable = false, length = 30)
    private String phoneNumber;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(nullable = false, length = 80)
    private String city;

    @Column(name = "state_region", nullable = false, length = 80)
    private String stateRegion;

    @Column(nullable = false, length = 80)
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role = Role.USER;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "verified_city", length = 80)
    private String verifiedCity;

    @Column(name = "verified_state_region", length = 80)
    private String verifiedStateRegion;

    @Column(name = "verified_country", length = 80)
    private String verifiedCountry;

    @Column(name = "location_verified_at")
    private Instant locationVerifiedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_source", length = 30)
    private LocationSource locationSource;

    @Column(name = "hidden_chats_pin_hash")
    private String hiddenChatsPinHash;

    @Column(name = "hidden_chats_pin_failed_attempts", nullable = false)
    private int hiddenChatsPinFailedAttempts;

    @Column(name = "hidden_chats_pin_locked_until")
    private Instant hiddenChatsPinLockedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AppUser() {
    }

    public AppUser(
            String username,
            String firstName,
            String lastName,
            String email,
            String passwordHash,
            String phoneNumber,
            LocalDate dateOfBirth,
            String city,
            String stateRegion,
            String country
    ) {
        this.username = username.trim().toLowerCase();
        this.firstName = firstName.trim();
        this.lastName = lastName.trim();
        this.email = email.trim().toLowerCase();
        this.passwordHash = passwordHash;
        this.phoneNumber = phoneNumber.trim();
        this.dateOfBirth = dateOfBirth;
        this.city = city.trim();
        // State/region is optional because not every country uses states or provinces.
        this.stateRegion = stateRegion == null || stateRegion.isBlank()
                ? null
                : stateRegion.trim();
        this.country = country.trim();
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {updatedAt = Instant.now();}

    public UUID getId() {return id;}

    public String getUsername() {return username;}

    public String getFirstName() {return firstName;}

    public String getLastName() {return lastName;}

    public String getEmail() {return email;}

    public String getPasswordHash() {return passwordHash;}

    public String getPhoneNumber() {return phoneNumber;}

    public LocalDate getDateOfBirth() {return dateOfBirth;}

    public String getCity() {return city;}

    public String getStateRegion() {return stateRegion;}

    public String getCountry() {return country;}

    public Role getRole() {return role;}

    public boolean isEmailVerified() {return emailVerified;}

    public Instant getEmailVerifiedAt() {return emailVerifiedAt;}

    public String getVerifiedCity() {return verifiedCity;}

    public String getVerifiedStateRegion() {return verifiedStateRegion;}

    public String getVerifiedCountry() {return verifiedCountry;}

    public Instant getLocationVerifiedAt() {return locationVerifiedAt;}

    public LocationSource getLocationSource() {return locationSource;}

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void markEmailVerified(Instant verifiedAt) {
        if (verifiedAt == null) {
            throw new IllegalArgumentException("Email verification time is required");
        }

        emailVerified = true;
        emailVerifiedAt = verifiedAt;
    }

    public void changePassword(String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash is required");
        }

        this.passwordHash = passwordHash;
    }

    public void changeVerifiedEmail(String email, Instant verifiedAt) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (verifiedAt == null) {
            throw new IllegalArgumentException("Email verification time is required");
        }

        this.email = email.trim().toLowerCase();
        this.emailVerified = true;
        this.emailVerifiedAt = verifiedAt;
    }

    // Stores the location resolved from the user's device coordinates.
    public void verifyLocation(
            String verifiedCity,
            String verifiedStateRegion,
            String verifiedCountry,
            Instant verifiedAt
    ) {
        if (verifiedCity == null || verifiedCity.isBlank()) {
            throw new IllegalArgumentException("Verified city is required");
        }

        if (verifiedCountry == null || verifiedCountry.isBlank()) {
            throw new IllegalArgumentException("Verified country is required");
        }

        if (verifiedAt == null) {
            throw new IllegalArgumentException("Location verification time is required");
        }

        this.verifiedCity = verifiedCity.trim();
        this.verifiedStateRegion = verifiedStateRegion == null || verifiedStateRegion.isBlank()
                ? null
                : verifiedStateRegion.trim();
        this.verifiedCountry = verifiedCountry.trim();
        this.locationVerifiedAt = verifiedAt;
        this.locationSource = LocationSource.DEVICE;
    }

    // Confirms whether the user has a complete verified location snapshot.
    public boolean hasVerifiedLocation() {
        return verifiedCity != null
                && verifiedCountry != null
                && locationVerifiedAt != null
                && locationSource != null;
    }

    public boolean hasHiddenChatsPin() {
        return hiddenChatsPinHash != null;
    }

    public void setHiddenChatsPin(String pinHash) {
        if (pinHash == null || pinHash.isBlank()) {
            throw new IllegalArgumentException("PIN hash is required");
        }

        this.hiddenChatsPinHash = pinHash;
        this.hiddenChatsPinFailedAttempts = 0;
        this.hiddenChatsPinLockedUntil = null;
    }

    public String getHiddenChatsPinHash() {
        return hiddenChatsPinHash;
    }

    public boolean isHiddenChatsPinLocked(Instant now) {
        return hiddenChatsPinLockedUntil != null && hiddenChatsPinLockedUntil.isAfter(now);
    }

    // Locks the PIN out for lockoutDuration once maxAttempts consecutive failures are
    // reached, then resets the counter so the next window starts clean once it expires.
    public void recordHiddenChatsPinFailure(Instant now, int maxAttempts, Duration lockoutDuration) {
        hiddenChatsPinFailedAttempts++;
        if (hiddenChatsPinFailedAttempts >= maxAttempts) {
            hiddenChatsPinLockedUntil = now.plus(lockoutDuration);
            hiddenChatsPinFailedAttempts = 0;
        }
    }

    public void recordHiddenChatsPinSuccess() {
        hiddenChatsPinFailedAttempts = 0;
        hiddenChatsPinLockedUntil = null;
    }
}
