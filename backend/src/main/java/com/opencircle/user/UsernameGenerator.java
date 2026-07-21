package com.opencircle.user;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
class UsernameGenerator {

    private static final String[] ADJECTIVES = {
            "swift", "bright", "calm", "bold", "quiet", "sunny"
    };

    private static final String[] NOUNS = {
            "falcon", "river", "maple", "comet", "harbor", "ember"
    };

    private static final int MAX_ATTEMPTS = 20;
    private final SecureRandom random = new SecureRandom();
    private final UserRepository users;

    UsernameGenerator(UserRepository users) {
        this.users = users;
    }

    String generate() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String username = generateCandidate();

            if (!users.existsByUsernameIgnoreCase(username)) {
                return username;
            }
        }

        throw new IllegalStateException("Could not generate a unique username");
    }

    private String generateCandidate() {
        String adjective = ADJECTIVES[random.nextInt(ADJECTIVES.length)];
        String noun = NOUNS[random.nextInt(NOUNS.length)];
        int suffix = random.nextInt(9000) + 1000;

        return adjective + "_" + noun + "_" + suffix;
    }
}
