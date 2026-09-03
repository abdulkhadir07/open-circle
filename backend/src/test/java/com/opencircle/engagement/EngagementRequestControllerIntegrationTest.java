package com.opencircle.engagement;

import com.opencircle.AbstractIntegrationTest;
import com.opencircle.invitepost.InvitePost;
import com.opencircle.invitepost.InvitePostRepository;
import com.opencircle.invitepost.InviteType;
import com.opencircle.invitepost.LocationScope;
import com.opencircle.user.AppUser;
import com.opencircle.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EngagementRequestControllerIntegrationTest extends AbstractIntegrationTest {

    private static final Instant VERIFIED_AT = Instant.parse("2026-08-30T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private InvitePostRepository posts;

    @Autowired
    private EngagementRequestRepository requests;

    @Test
    void createRequestReturnsCreatedEngagementRequest() throws Exception {
        AppUser poster = verifiedUser("poster.engage@example.com", "San Francisco", "California", "USA");
        AppUser requester = verifiedUser("requester.engage@example.com", "San Francisco", "California", "USA");
        InvitePost post = posts.save(invitePost(poster, "Anyone want coffee?", InviteType.GROUP, 3));

        String token = loginToken("requester.engage@example.com");

        mockMvc.perform(post("/api/invite-posts/{postId}/engagements", post.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invitePostId").value(post.getId().toString()))
                .andExpect(jsonPath("$.requesterId").value(requester.getId().toString()))
                .andExpect(jsonPath("$.requesterUsername").value(requester.getUsername()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.expiresAt").value(post.getExpiresAt().toString()));
    }

    @Test
    void createRequestRejectsOwnPost() throws Exception {
        AppUser poster = verifiedUser("poster.self@example.com", "San Francisco", "California", "USA");
        InvitePost post = posts.save(invitePost(poster, "Owner should not engage this", InviteType.GROUP, 3));

        String token = loginToken("poster.self@example.com");

        mockMvc.perform(post("/api/invite-posts/{postId}/engagements", post.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You cannot engage with your own invite post"));
    }

    @Test
    void posterCanListRequestsForTheirPost() throws Exception {
        AppUser poster = verifiedUser("poster.list@example.com", "San Francisco", "California", "USA");
        AppUser requester = verifiedUser("requester.list@example.com", "San Francisco", "California", "USA");
        InvitePost post = posts.save(invitePost(poster, "List my requests", InviteType.GROUP, 3));
        requests.save(new EngagementRequest(post, requester, Instant.now()));

        String token = loginToken("poster.list@example.com");

        mockMvc.perform(get("/api/invite-posts/{postId}/engagements", post.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].requesterId").value(requester.getId().toString()))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void nonPosterCannotListRequestsForPost() throws Exception {
        AppUser poster = verifiedUser("poster.private@example.com", "San Francisco", "California", "USA");
        AppUser requester = verifiedUser("requester.private@example.com", "San Francisco", "California", "USA");
        InvitePost post = posts.save(invitePost(poster, "Private requests", InviteType.GROUP, 3));

        String token = loginToken("requester.private@example.com");

        mockMvc.perform(get("/api/invite-posts/{postId}/engagements", post.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only the post owner can manage engagement requests"));
    }

    @Test
    void posterCanAcceptRequestCapacityDecreasesAndChatRoomOpens() throws Exception {
        AppUser poster = verifiedUser("poster.accept.controller@example.com", "San Francisco", "California", "USA");
        AppUser requester = verifiedUser("requester.accept.controller@example.com", "San Francisco", "California", "USA");
        InvitePost post = posts.save(invitePost(poster, "Accept me", InviteType.GROUP, 3));
        EngagementRequest request = requests.save(new EngagementRequest(post, requester, Instant.now()));

        String posterToken = loginToken("poster.accept.controller@example.com");
        String requesterToken = loginToken("requester.accept.controller@example.com");

        mockMvc.perform(patch("/api/engagements/{requestId}/accept", request.getId())
                        .header("Authorization", "Bearer " + posterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.respondedAt").exists());

        InvitePost updatedPost = posts.findById(post.getId()).orElseThrow();
        assertThat(updatedPost.getAcceptedCount()).isEqualTo(1);
        assertThat(updatedPost.getInvitesLeft()).isEqualTo(2);

        mockMvc.perform(get("/api/chat-rooms")
                        .header("Authorization", "Bearer " + requesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].invitePostId").value(post.getId().toString()))
                .andExpect(jsonPath("$[0].invitePostContent").value("Accept me"))
                .andExpect(jsonPath("$[0].participants", hasSize(2)));
    }

    @Test
    void posterCanDeclineRequest() throws Exception {
        AppUser poster = verifiedUser("poster.decline.controller@example.com", "San Francisco", "California", "USA");
        AppUser requester = verifiedUser("requester.decline.controller@example.com", "San Francisco", "California", "USA");
        InvitePost post = posts.save(invitePost(poster, "Decline me", InviteType.GROUP, 3));
        EngagementRequest request = requests.save(new EngagementRequest(post, requester, Instant.now()));

        String token = loginToken("poster.decline.controller@example.com");

        mockMvc.perform(patch("/api/engagements/{requestId}/decline", request.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECLINED"))
                .andExpect(jsonPath("$.respondedAt").exists());
    }

    @Test
    void posterCanHoldRequest() throws Exception {
        AppUser poster = verifiedUser("poster.hold.controller@example.com", "San Francisco", "California", "USA");
        AppUser requester = verifiedUser("requester.hold.controller@example.com", "San Francisco", "California", "USA");
        InvitePost post = posts.save(invitePost(poster, "Hold me", InviteType.GROUP, 3));
        EngagementRequest request = requests.save(new EngagementRequest(post, requester, Instant.now()));

        String token = loginToken("poster.hold.controller@example.com");

        mockMvc.perform(patch("/api/engagements/{requestId}/hold", request.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HELD"))
                .andExpect(jsonPath("$.respondedAt").exists());
    }

    @Test
    void requesterCanWithdrawPendingRequest() throws Exception {
        AppUser poster = verifiedUser("poster.withdraw.controller@example.com", "San Francisco", "California", "USA");
        AppUser requester = verifiedUser("requester.withdraw.controller@example.com", "San Francisco", "California", "USA");
        InvitePost post = posts.save(invitePost(poster, "Withdraw me", InviteType.GROUP, 3));
        EngagementRequest request = requests.save(new EngagementRequest(post, requester, Instant.now()));

        String token = loginToken("requester.withdraw.controller@example.com");

        mockMvc.perform(patch("/api/engagements/{requestId}/withdraw", request.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WITHDRAWN"))
                .andExpect(jsonPath("$.withdrawnAt").exists());
    }

    private String loginToken(String email) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "Password123!"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return response.split("\"token\":\"")[1].split("\"")[0];
    }

    private AppUser verifiedUser(
            String email,
            String city,
            String stateRegion,
            String country
    ) {
        AppUser user = users.createUser(
                "Test",
                "User",
                email,
                passwordEncoder.encode("Password123!"),
                phoneNumber(email),
                LocalDate.of(2000, 1, 1),
                city,
                stateRegion,
                country
        );

        user.markEmailVerified(VERIFIED_AT);
        user.verifyLocation(city, stateRegion, country, VERIFIED_AT);

        return user;
    }

    private InvitePost invitePost(
            AppUser poster,
            String content,
            InviteType inviteType,
            int totalCapacity
    ) {
        return new InvitePost(
                poster,
                content,
                inviteType,
                totalCapacity,
                LocationScope.CITY,
                poster.getVerifiedCity(),
                poster.getVerifiedStateRegion(),
                poster.getVerifiedCountry(),
                Instant.now()
        );
    }

    private String phoneNumber(String email) {
        long suffix = Integer.toUnsignedLong(email.hashCode()) % 10_000_000_000L;
        return "+1%010d".formatted(suffix);
    }
}