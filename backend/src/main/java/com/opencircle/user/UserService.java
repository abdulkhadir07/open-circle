package com.opencircle.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository users;
    private final UsernameGenerator usernameGenerator;

    UserService(UserRepository users, UsernameGenerator usernameGenerator) {
        this.users = users;
        this.usernameGenerator = usernameGenerator;
    }

    @Transactional
    public AppUser createUser(
            String firstName,
            String lastName,
            String email,
            String passwordHash,
            String phoneNumber,
            java.time.LocalDate dateOfBirth,
            String city,
            String stateRegion,
            String country
    ) {
        String username = usernameGenerator.generate();

        AppUser user = new AppUser(
                username,
                firstName,
                lastName,
                email,
                passwordHash,
                phoneNumber,
                dateOfBirth,
                city,
                stateRegion,
                country
        );

        return users.save(user);
    }

    @Transactional(readOnly = true)
    public AppUser getById(UUID id) {
        return users.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @Transactional(readOnly = true)
    public AppUser getByEmail(String email) {
        return users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
    }

    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return users.existsByEmailIgnoreCase(email);
    }

    @Transactional(readOnly = true)
    public boolean phoneNumberExists(String phoneNumber) {
        return users.existsByPhoneNumber(phoneNumber);
    }
}
