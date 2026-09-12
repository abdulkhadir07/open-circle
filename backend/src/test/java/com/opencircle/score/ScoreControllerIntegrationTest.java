package com.opencircle.score;

import com.opencircle.AbstractIntegrationTest;
import com.opencircle.profileimage.ProfileImageQueryService;
import com.opencircle.profileimage.ProfileImageResponse;
import com.opencircle.user.AppUser;
import com.opencircle.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ScoreControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "Password123!";
    private static final Instant NOW = Instant.parse("2026-09-11T12:00:00Z");
    private static final Instant AWARD_FINALIZED_AT = Instant.parse("2026-01-01T00:05:00Z");

    @Autowired private MockMvc mockMvc;
    @Autowired private UserService users;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbc;

    @MockitoBean private CircleScoreQueryRepository scores;
    @MockitoBean private ProfileImageQueryService profileImages;
    @MockitoBean private Clock clock;

    @BeforeEach
    void fixedCurrentSeason() {
        when(clock.instant()).thenReturn(NOW);
        when(profileImages.getProfileImagesByUserIds(any())).thenReturn(Map.of());
    }

    @Test
    void returnsPrivateScoreAndCurrentTopFiveRankEntries() throws Exception {
        AppUser currentUser = verifiedUser("score.current@example.com");
        String token = loginToken(currentUser.getEmail());
        UUID firstUserId = UUID.randomUUID();
        UUID tiedUserId = UUID.randomUUID();

        when(scores.findSummary(currentUser.getId(), 2026))
                .thenReturn(new CircleScoreSummary(2026, -7, 18));
        when(scores.findTopRanks(2026, 5)).thenReturn(List.of(
                new RankedScoreboardEntry(
                        1,
                        firstUserId,
                        "circle-first",
                        24,
                        new BigDecimal("4.75"),
                        3
                ),
                new RankedScoreboardEntry(
                        5,
                        tiedUserId,
                        "circle-fifth",
                        8,
                        new BigDecimal("4.00"),
                        1
                )
        ));

        mockMvc.perform(get("/api/users/me/score")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(currentUser.getId().toString()))
                .andExpect(jsonPath("$.seasonYear").value(2026))
                .andExpect(jsonPath("$.annualScore").value(-7))
                .andExpect(jsonPath("$.lifetimeScore").value(18));

        mockMvc.perform(get("/api/scoreboard")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seasonYear").value(2026))
                .andExpect(jsonPath("$.entries.length()").value(2))
                .andExpect(jsonPath("$.entries[0].rank").value(1))
                .andExpect(jsonPath("$.entries[0].userId").value(firstUserId.toString()))
                .andExpect(jsonPath("$.entries[0].username").value("circle-first"))
                .andExpect(jsonPath("$.entries[0].profileImage").doesNotExist())
                .andExpect(jsonPath("$.entries[0].annualScore").value(24))
                .andExpect(jsonPath("$.entries[0].averageRating").value(4.75))
                .andExpect(jsonPath("$.entries[0].currentYearDistinctRaterCount").value(3))
                .andExpect(jsonPath("$.entries[1].rank").value(5));

        verify(profileImages).getProfileImagesByUserIds(Set.of(firstUserId, tiedUserId));
    }

    @Test
    void scoreEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/me/score"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/scoreboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsAStoredAnnualAwardWithOneBatchedProfileImageLookup() throws Exception {
        AppUser currentUser = verifiedUser("annual-award-viewer@example.com");
        AppUser winner = verifiedUser("annual-award-winner@example.com");
        String token = loginToken(currentUser.getEmail());
        UUID profileImageId = UUID.randomUUID();
        Instant imageExpiry = Instant.parse("2026-01-01T01:05:00Z");
        Set<UUID> winnerIds = Set.of(winner.getId());

        insertAwardFinalization(2025);
        jdbc.update(
                """
                INSERT INTO annual_awards (
                    id, season_year, winner_user_id, final_score, awarded_at
                )
                VALUES (?, 2025, ?, 55, ?)
                """,
                UUID.randomUUID(),
                winner.getId(),
                Timestamp.from(AWARD_FINALIZED_AT)
        );
        when(profileImages.getProfileImagesByUserIds(winnerIds)).thenReturn(Map.of(
                winner.getId(),
                new ProfileImageResponse(
                        profileImageId,
                        "https://example.com/profile.jpg",
                        imageExpiry,
                        "image/jpeg",
                        AWARD_FINALIZED_AT
                )
        ));

        mockMvc.perform(get("/api/awards/2025")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seasonYear").value(2025))
                .andExpect(jsonPath("$.name").value("Circle Champion 2025"))
                .andExpect(jsonPath("$.finalizedAt").value(AWARD_FINALIZED_AT.toString()))
                .andExpect(jsonPath("$.winners.length()").value(1))
                .andExpect(jsonPath("$.winners[0].userId").value(winner.getId().toString()))
                .andExpect(jsonPath("$.winners[0].username").value(winner.getUsername()))
                .andExpect(jsonPath("$.winners[0].finalScore").value(55))
                .andExpect(jsonPath("$.winners[0].profileImage.id")
                        .value(profileImageId.toString()));

        verify(profileImages).getProfileImagesByUserIds(winnerIds);
    }

    @Test
    void representsACompletedNoWinnerSeasonAndRejectsUnfinalizedOrUnauthenticatedReads()
            throws Exception {
        AppUser currentUser = verifiedUser("annual-award-empty-viewer@example.com");
        String token = loginToken(currentUser.getEmail());
        insertAwardFinalization(2024);

        mockMvc.perform(get("/api/awards/2024")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seasonYear").value(2024))
                .andExpect(jsonPath("$.name").value("Circle Champion 2024"))
                .andExpect(jsonPath("$.winners").isEmpty());

        verify(profileImages).getProfileImagesByUserIds(Set.of());

        mockMvc.perform(get("/api/awards/2023")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Annual award has not been finalized for 2023"));

        mockMvc.perform(get("/api/awards/2025"))
                .andExpect(status().isUnauthorized());
    }

    private void insertAwardFinalization(int seasonYear) {
        jdbc.update(
                """
                INSERT INTO annual_award_finalizations (season_year, finalized_at)
                VALUES (?, ?)
                """,
                seasonYear,
                Timestamp.from(AWARD_FINALIZED_AT)
        );
    }

    private AppUser verifiedUser(String email) {
        AppUser user = users.createUser(
                "Score",
                "User",
                email,
                passwordEncoder.encode(PASSWORD),
                phoneNumber(email),
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );
        user.markEmailVerified(NOW);
        user.verifyLocation("San Francisco", "California", "USA", NOW);
        return user;
    }

    private String loginToken(String email) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return response.split("\"token\":\"")[1].split("\"")[0];
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String phoneNumber(String email) {
        long suffix = Integer.toUnsignedLong(email.hashCode()) % 10_000_000_000L;
        return "+1%010d".formatted(suffix);
    }
}
