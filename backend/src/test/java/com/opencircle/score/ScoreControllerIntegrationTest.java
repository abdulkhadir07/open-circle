package com.opencircle.score;

import com.opencircle.AbstractIntegrationTest;
import com.opencircle.profileimage.ProfileImageQueryService;
import com.opencircle.user.AppUser;
import com.opencircle.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    @Autowired private MockMvc mockMvc;
    @Autowired private UserService users;
    @Autowired private PasswordEncoder passwordEncoder;

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
