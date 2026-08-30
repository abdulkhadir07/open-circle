package com.opencircle.user.dto;

import com.opencircle.user.AppUser;
import com.opencircle.user.Role;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        LocalDate dateOfBirth,
        String city,
        String stateRegion,
        String country,
        String verifiedCity,
        String verifiedStateRegion,
        String verifiedCountry,
        Instant locationVerifiedAt,
        String locationSource,
        Role role,
        boolean emailVerified,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserResponse from(AppUser user) {
        // Builds the API-safe user profile returned to clients.
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getDateOfBirth(),
                user.getCity(),
                user.getStateRegion(),
                user.getCountry(),
                user.getVerifiedCity(),
                user.getVerifiedStateRegion(),
                user.getVerifiedCountry(),
                user.getLocationVerifiedAt(),
                user.getLocationSource() == null ? null : user.getLocationSource().name(),
                user.getRole(),
                user.isEmailVerified(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
