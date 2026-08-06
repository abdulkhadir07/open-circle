package com.opencircle.user;

import jakarta.persistence.*;

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
        this.stateRegion = stateRegion.trim();
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
}
