package com.opencircle.user;

import com.opencircle.AbstractIntegrationTest;
import com.opencircle.security.JwtService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.notNullValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerIntegrationTest extends AbstractIntegrationTest {

    private static final AtomicInteger USER_SEQUENCE = new AtomicInteger(7000);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserProfileRepository profiles;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager entityManager;

    @Test
    void meReturnsCurrentUserWhenAuthenticated() throws Exception {
        AppUser user = userService.createUser(
                "Current",
                "User",
                "current.user@example.com",
                "hashed-password",
                "+14155550300",
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );

        String token = jwtService.generateToken(user);

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.username", notNullValue()))
                .andExpect(jsonPath("$.firstName").value("Current"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.email").value("current.user@example.com"))
                .andExpect(jsonPath("$.phoneNumber").value("+14155550300"))
                .andExpect(jsonPath("$.dateOfBirth").value("2000-01-01"))
                .andExpect(jsonPath("$.city").value("San Francisco"))
                .andExpect(jsonPath("$.stateRegion").value("California"))
                .andExpect(jsonPath("$.country").value("USA"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.emailVerified").value(false))
                .andExpect(jsonPath("$.hasHiddenChatsPin").value(false));
    }

    @Test
    void meRejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void publicAndCurrentUserProfileRoutesReturnTheSamePublicSafeShape() throws Exception {
        AppUser viewer = user("viewer");
        AppUser profileOwner = user("owner");
        String token = jwtService.generateToken(viewer);
        Instant awardedAt = Instant.parse("2026-01-01T00:05:00Z");
        entityManager.flush();
        insertAward(profileOwner, 2001, 70, awardedAt);
        insertAward(profileOwner, 2000, 55, awardedAt.minusSeconds(365L * 24 * 60 * 60));

        mockMvc.perform(get("/api/users/{userId}/profile", profileOwner.getId())
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(profileOwner.getId().toString()))
                .andExpect(jsonPath("$.username").value(profileOwner.getUsername()))
                .andExpect(jsonPath("$.displayName").value("Profile"))
                .andExpect(jsonPath("$.profileImage").isEmpty())
                .andExpect(jsonPath("$.bio").isEmpty())
                .andExpect(jsonPath("$.interests").isEmpty())
                .andExpect(jsonPath("$.memberSince").exists())
                .andExpect(jsonPath("$.reputation.averageRating").isEmpty())
                .andExpect(jsonPath("$.reputation.totalRatingsReceived").value(0))
                .andExpect(jsonPath("$.reputation.distinctRaterCount").value(0))
                .andExpect(jsonPath("$.awards.length()").value(2))
                .andExpect(jsonPath("$.awards[0].seasonYear").value(2001))
                .andExpect(jsonPath("$.awards[0].name").value("Circle Champion 2001"))
                .andExpect(jsonPath("$.awards[0].finalScore").value(70))
                .andExpect(jsonPath("$.awards[0].awardedAt").value(awardedAt.toString()))
                .andExpect(jsonPath("$.awards[1].seasonYear").value(2000))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.phoneNumber").doesNotExist())
                .andExpect(jsonPath("$.dateOfBirth").doesNotExist())
                .andExpect(jsonPath("$.firstName").doesNotExist())
                .andExpect(jsonPath("$.lastName").doesNotExist())
                .andExpect(jsonPath("$.city").doesNotExist())
                .andExpect(jsonPath("$.stateRegion").doesNotExist())
                .andExpect(jsonPath("$.country").doesNotExist())
                .andExpect(jsonPath("$.verifiedCity").doesNotExist())
                .andExpect(jsonPath("$.locationVerifiedAt").doesNotExist())
                .andExpect(jsonPath("$.role").doesNotExist())
                .andExpect(jsonPath("$.annualScore").doesNotExist())
                .andExpect(jsonPath("$.lifetimeScore").doesNotExist());

        mockMvc.perform(get("/api/users/me/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(viewer.getId().toString()))
                .andExpect(jsonPath("$.displayName").value("Profile"))
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    void currentUserCanReplaceAndClearOptionalProfileFields() throws Exception {
        AppUser user = user("update");
        String token = jwtService.generateToken(user);

        mockMvc.perform(put("/api/users/me/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "  Curious Neighbor  ",
                                  "bio": "  Always up for something new.  ",
                                  "interests": ["  Hiking  ", "Coffee", "Live Music"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Curious Neighbor"))
                .andExpect(jsonPath("$.bio").value("Always up for something new."))
                .andExpect(jsonPath("$.interests[0]").value("Hiking"))
                .andExpect(jsonPath("$.interests[1]").value("Coffee"))
                .andExpect(jsonPath("$.interests[2]").value("Live Music"));

        UserProfile saved = profiles.findForDisplay(user.getId()).orElseThrow();
        assertThat(saved.getInterests()).containsExactly("Hiking", "Coffee", "Live Music");

        mockMvc.perform(put("/api/users/me/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Curious Neighbor",
                                  "bio": "   ",
                                  "interests": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").isEmpty())
                .andExpect(jsonPath("$.interests").isEmpty());
    }

    @Test
    void profileReplacementRejectsDuplicateBlankAndMissingInterests() throws Exception {
        AppUser user = user("invalid-update");
        String authorization = "Bearer " + jwtService.generateToken(user);

        mockMvc.perform(put("/api/users/me/profile")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Profile",
                                  "bio": null,
                                  "interests": ["Hiking", "hIkInG"]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Interests must be unique ignoring case"));

        mockMvc.perform(put("/api/users/me/profile")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Profile",
                                  "bio": null,
                                  "interests": ["Coffee", "   "]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));

        mockMvc.perform(put("/api/users/me/profile")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Profile",
                                  "bio": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.interests")
                        .value("Interests are required; use an empty list for none"));
    }

    @Test
    void profileRoutesReturnNotFoundAndRequireAuthentication() throws Exception {
        AppUser user = user("auth");
        String token = jwtService.generateToken(user);

        mockMvc.perform(get("/api/users/{userId}/profile", UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User profile not found"));

        mockMvc.perform(get("/api/users/{userId}/profile", user.getId()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users/me/profile"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Profile",
                                  "bio": null,
                                  "interests": []
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    private void insertAward(AppUser winner, int seasonYear, long score, Instant awardedAt) {
        jdbc.update(
                "INSERT INTO annual_award_finalizations (season_year, finalized_at) VALUES (?, ?)",
                seasonYear,
                Timestamp.from(awardedAt)
        );
        jdbc.update(
                """
                INSERT INTO annual_awards (id, season_year, winner_user_id, final_score, awarded_at)
                VALUES (?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                seasonYear,
                winner.getId(),
                score,
                Timestamp.from(awardedAt)
        );
    }

    private AppUser user(String label) {
        int sequence = USER_SEQUENCE.incrementAndGet();
        return userService.createUser(
                "Profile",
                "User",
                label + ".user.profile." + sequence + "@example.com",
                "hashed-password",
                "+1415555" + sequence,
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );
    }
}
