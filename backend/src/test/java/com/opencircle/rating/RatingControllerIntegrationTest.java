package com.opencircle.rating;

import com.jayway.jsonpath.JsonPath;
import com.opencircle.AbstractIntegrationTest;
import com.opencircle.engagement.EngagementRequest;
import com.opencircle.invitepost.InvitePost;
import com.opencircle.invitepost.InvitePostRepository;
import com.opencircle.invitepost.InviteType;
import com.opencircle.invitepost.LocationScope;
import com.opencircle.profileimage.ProfileImageQueryService;
import com.opencircle.user.AppUser;
import com.opencircle.user.UserService;
import jakarta.persistence.EntityManager;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RatingControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "Password123!";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserService users;
    @Autowired private InvitePostRepository posts;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    @MockitoBean
    private ProfileImageQueryService profileImageQueryService;

    @BeforeEach
    void missingProfileImagesByDefault() {
        when(profileImageQueryService.getProfileImagesByUserIds(any())).thenReturn(Map.of());
    }

    @Test
    void qualifiedParticipantsCanSubmitSealedRatingsThatRevealTogether() throws Exception {
        AppUser poster = verifiedUser("poster.rating-flow@example.com");
        AppUser requester = verifiedUser("requester.rating-flow@example.com");
        AcceptedInteraction interaction = acceptedInteraction(poster, requester, "Sealed rating flow");

        sendMessage(interaction.roomId(), interaction.posterToken(), "Poster starts");
        sendMessage(interaction.roomId(), interaction.requesterToken(), "Requester replies");
        sendMessage(interaction.roomId(), interaction.posterToken(), "Poster follows up");

        mockMvc.perform(patch("/api/chat-rooms/{roomId}/leave", interaction.roomId())
                        .header("Authorization", bearer(interaction.requesterToken())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/me/ratings/due")
                        .header("Authorization", bearer(interaction.posterToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].engagementId").value(interaction.engagementId().toString()))
                .andExpect(jsonPath("$[0].otherUserId").value(requester.getId().toString()))
                .andExpect(jsonPath("$[0].otherUsername").value(requester.getUsername()))
                .andExpect(jsonPath("$[0].trigger").value("PARTICIPANT_EXIT"));

        mockMvc.perform(post("/api/engagements/{engagementId}/ratings", interaction.engagementId())
                        .header("Authorization", bearer(interaction.posterToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.score").value(5))
                .andExpect(jsonPath("$.revealed").value(false))
                .andExpect(jsonPath("$.revealedAt").doesNotExist());

        mockMvc.perform(post("/api/engagements/{engagementId}/ratings", interaction.engagementId())
                        .header("Authorization", bearer(interaction.posterToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Rating has already been submitted"));

        mockMvc.perform(get("/api/users/me/ratings/received")
                        .header("Authorization", bearer(interaction.requesterToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ratings", hasSize(0)));

        mockMvc.perform(post("/api/engagements/{engagementId}/ratings", interaction.engagementId())
                        .header("Authorization", bearer(interaction.requesterToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":4}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.score").value(4))
                .andExpect(jsonPath("$.revealed").value(true))
                .andExpect(jsonPath("$.revealedAt").exists());

        mockMvc.perform(get("/api/users/me/ratings/received")
                        .header("Authorization", bearer(interaction.requesterToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ratings", hasSize(1)))
                .andExpect(jsonPath("$.ratings[0].raterUserId").value(poster.getId().toString()))
                .andExpect(jsonPath("$.ratings[0].score").value(5));

        mockMvc.perform(get("/api/users/{userId}/reputation", requester.getId())
                        .header("Authorization", bearer(interaction.posterToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(requester.getId().toString()))
                .andExpect(jsonPath("$.averageRating").value(5.0))
                .andExpect(jsonPath("$.totalRatingsReceived").value(1))
                .andExpect(jsonPath("$.distinctRaterCount").value(1));
    }

    @Test
    void submissionValidatesScoreAndRejectsOutsiders() throws Exception {
        AppUser poster = verifiedUser("poster.rating-errors@example.com");
        AppUser requester = verifiedUser("requester.rating-errors@example.com");
        AppUser outsider = verifiedUser("outsider.rating-errors@example.com");
        AcceptedInteraction interaction = acceptedInteraction(poster, requester, "Rating errors");
        String outsiderToken = loginToken(outsider.getEmail());

        mockMvc.perform(post("/api/engagements/{engagementId}/ratings", interaction.engagementId())
                        .header("Authorization", bearer(interaction.posterToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":6}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/engagements/{engagementId}/ratings", interaction.engagementId())
                        .header("Authorization", bearer(outsiderToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":5}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("Only engagement participants can rate this interaction"));

        mockMvc.perform(post("/api/engagements/{engagementId}/ratings", interaction.engagementId())
                        .header("Authorization", bearer(interaction.posterToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":5}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Rating is not required yet"));
    }

    @Test
    void acceptedEngagementWithoutEnrollmentIsNotRetroactivelyRateable() throws Exception {
        AppUser poster = verifiedUser("poster.historical-rating@example.com");
        AppUser requester = verifiedUser("requester.historical-rating@example.com");
        InvitePost post = posts.save(invitePost(poster, "Historical accepted engagement"));
        EngagementRequest engagement = new EngagementRequest(post, requester, Instant.now().minusSeconds(60));
        entityManager.persist(engagement);
        engagement.accept(Instant.now());
        post.recordAcceptedEngagement();
        entityManager.flush();

        mockMvc.perform(post("/api/engagements/{engagementId}/ratings", engagement.getId())
                        .header("Authorization", bearer(loginToken(poster.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":5}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Rating is not available for this engagement"));
    }

    @Test
    void ratingEndpointsRequireAuthentication() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(get("/api/users/me/ratings/due"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users/me/ratings/received"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users/{userId}/reputation", id))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/engagements/{engagementId}/ratings", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":5}"))
                .andExpect(status().isUnauthorized());
    }

    private AcceptedInteraction acceptedInteraction(AppUser poster, AppUser requester, String content) throws Exception {
        InvitePost post = posts.save(invitePost(poster, content));
        String posterToken = loginToken(poster.getEmail());
        String requesterToken = loginToken(requester.getEmail());

        MvcResult createResult = mockMvc.perform(post("/api/invite-posts/{postId}/engagements", post.getId())
                        .header("Authorization", bearer(requesterToken)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID engagementId = UUID.fromString(JsonPath.read(
                createResult.getResponse().getContentAsString(),
                "$.id"
        ));

        mockMvc.perform(patch("/api/engagements/{engagementId}/accept", engagementId)
                        .header("Authorization", bearer(posterToken)))
                .andExpect(status().isOk());

        MvcResult roomsResult = mockMvc.perform(get("/api/chat-rooms")
                        .header("Authorization", bearer(requesterToken)))
                .andExpect(status().isOk())
                .andReturn();
        UUID roomId = UUID.fromString(JsonPath.read(
                roomsResult.getResponse().getContentAsString(),
                "$[0].id"
        ));

        return new AcceptedInteraction(engagementId, roomId, posterToken, requesterToken);
    }

    private void sendMessage(UUID roomId, String token, String body) throws Exception {
        mockMvc.perform(post("/api/chat-rooms/{roomId}/messages", roomId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"%s\"}".formatted(body)))
                .andExpect(status().isCreated());
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

    private AppUser verifiedUser(String email) {
        AppUser user = users.createUser(
                "Rating",
                "User",
                email,
                passwordEncoder.encode(PASSWORD),
                phoneNumber(email),
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );
        Instant now = Instant.now();
        user.markEmailVerified(now);
        user.verifyLocation("San Francisco", "California", "USA", now);
        return user;
    }

    private InvitePost invitePost(AppUser poster, String content) {
        return new InvitePost(
                poster,
                content,
                InviteType.GROUP,
                3,
                LocationScope.CITY,
                "San Francisco",
                "California",
                "USA",
                Instant.now()
        );
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String phoneNumber(String email) {
        long suffix = Integer.toUnsignedLong(email.hashCode()) % 10_000_000_000L;
        return "+1%010d".formatted(suffix);
    }

    private record AcceptedInteraction(
            UUID engagementId,
            UUID roomId,
            String posterToken,
            String requesterToken
    ) {
    }
}
