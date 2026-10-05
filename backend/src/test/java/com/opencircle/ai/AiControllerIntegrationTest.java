package com.opencircle.ai;

import com.opencircle.AbstractIntegrationTest;
import com.opencircle.invitepost.InvitePost;
import com.opencircle.invitepost.InvitePostRepository;
import com.opencircle.invitepost.InviteType;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = "app.ai.requests-per-minute=3")
class AiControllerIntegrationTest extends AbstractIntegrationTest {

    private static final Instant VERIFIED_AT = Instant.parse("2026-08-30T00:00:00Z");

    @Autowired private MockMvc mockMvc;
    @Autowired private UserService users;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private InvitePostRepository posts;

    @MockitoBean private AiClient aiClient;
    @MockitoBean private ProfileImageQueryService profileImageQueryService;

    @BeforeEach
    void defaults() {
        when(profileImageQueryService.getProfileImagesByUserIds(any())).thenReturn(Map.of());
        when(aiClient.isEnabled()).thenReturn(false);
    }

    // ---- invite draft ----

    @Test
    void draftBuildsAnInviteFromRulesWhenThereIsNoKey() throws Exception {
        user("drafter@sfsu.edu");

        mockMvc.perform(draft("drafter@sfsu.edu", "need 4 players for pickup soccer tonight"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiGenerated").value(false))
                .andExpect(jsonPath("$.inviteType").value("GROUP"))
                .andExpect(jsonPath("$.totalCapacity").value(4))
                .andExpect(jsonPath("$.tags[0]").value("games"))
                .andExpect(jsonPath("$.content").value("Need 4 players for pickup soccer tonight"));
    }

    @Test
    void draftUsesTheModelWhenAvailable() throws Exception {
        user("drafter2@sfsu.edu");
        when(aiClient.isEnabled()).thenReturn(true);
        when(aiClient.generateJson(anyString(), anyDouble())).thenReturn("""
                {"content": "Anyone up for coffee at the student center around 3?", "inviteType": "SINGLE",
                 "totalCapacity": null, "tags": ["coffee"]}
                """);

        mockMvc.perform(draft("drafter2@sfsu.edu", "coffee at 3 anyone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiGenerated").value(true))
                .andExpect(jsonPath("$.inviteType").value("SINGLE"))
                .andExpect(jsonPath("$.totalCapacity").doesNotExist())
                .andExpect(jsonPath("$.content").value("Anyone up for coffee at the student center around 3?"));
    }

    @Test
    void draftRejectsBlankAndOversizedText() throws Exception {
        user("drafter3@sfsu.edu");

        mockMvc.perform(draft("drafter3@sfsu.edu", "   ")).andExpect(status().isBadRequest());
        mockMvc.perform(draft("drafter3@sfsu.edu", "x".repeat(501))).andExpect(status().isBadRequest());
    }

    @Test
    void draftIsRateLimitedPerUserWhenTheModelIsOn() throws Exception {
        user("drafter4@sfsu.edu");
        when(aiClient.isEnabled()).thenReturn(true);
        when(aiClient.generateJson(anyString(), anyDouble()))
                .thenReturn("{\"content\": \"x\", \"inviteType\": \"SINGLE\", \"tags\": []}");

        for (int attempt = 0; attempt < 3; attempt++) {
            mockMvc.perform(draft("drafter4@sfsu.edu", "coffee")).andExpect(status().isOk());
        }

        mockMvc.perform(draft("drafter4@sfsu.edu", "coffee"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("You're doing that a lot. Please wait a moment and try again."));
    }

    @Test
    void aiEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/ai/invite-draft").contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"hi\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/ai/feed-insights")).andExpect(status().isUnauthorized());
    }

    // ---- feed insights ----

    @Test
    void feedInsightsExplainsWhichInvitesMatchYourInterests() throws Exception {
        AppUser viewer = user("viewer@student.sfsu.edu");
        AppUser poster = user("poster@mail.sfsu.edu");
        AppUser stranger = user("stranger@stanford.edu");
        mockMvc.perform(put("/api/users/me/profile")
                        .header("Authorization", bearer("viewer@student.sfsu.edu"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\": \"Viewer\", \"bio\": \"Student\", \"interests\": [\"coffee\"]}"))
                .andExpect(status().isOk());

        Instant now = Instant.now();
        posts.save(new InvitePost(poster, "Coffee and a chat at the student center", InviteType.GROUP, 3, now.minusSeconds(60), List.of("coffee")));
        posts.save(new InvitePost(poster, "Chess in the library", InviteType.GROUP, 3, now.minusSeconds(30), List.of("games")));
        posts.save(new InvitePost(stranger, "Stanford coffee meetup", InviteType.GROUP, 3, now.minusSeconds(10), List.of("coffee")));
        posts.save(new InvitePost(viewer, "My own coffee invite", InviteType.GROUP, 3, now.minusSeconds(5), List.of("coffee")));

        mockMvc.perform(get("/api/ai/feed-insights").header("Authorization", bearer("viewer@student.sfsu.edu")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiGenerated").value(false))
                .andExpect(jsonPath("$.reasons", hasSize(1)))
                .andExpect(jsonPath("$.reasons[0].reason").value("Matches your interest in coffee"))
                .andExpect(jsonPath("$.digest").value(org.hamcrest.Matchers.startsWith("2 open invites on your campus today.")));
    }

    @Test
    void feedInsightsOnAnEmptyCampusSaysSo() throws Exception {
        user("lonely@sfsu.edu");

        mockMvc.perform(get("/api/ai/feed-insights").header("Authorization", bearer("lonely@sfsu.edu")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.digest").value("Quiet day so far. Be the first to start something."))
                .andExpect(jsonPath("$.reasons", hasSize(0)));
    }

    // ---- Safety Guardian ----

    @Test
    void anUnsafeInviteIsRejectedAndNothingIsSaved() throws Exception {
        user("poster@sfsu.edu");
        long before = posts.count();

        mockMvc.perform(post("/api/invite-posts")
                        .header("Authorization", bearer("poster@sfsu.edu"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"Buy me a gift card and I'll hang out\", \"inviteType\": \"SINGLE\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("That looks like a request for money or gift cards. Please keep payments out of OpenCircle."));

        assertThat(posts.count()).isEqualTo(before);
    }

    @Test
    void theModelCanRejectAnInviteTheRulesAllow() throws Exception {
        user("poster2@sfsu.edu");
        when(aiClient.isEnabled()).thenReturn(true);
        when(aiClient.generateJson(anyString(), anyDouble()))
                .thenReturn("{\"ok\": false, \"reason\": \"Please keep it friendly.\"}");

        mockMvc.perform(post("/api/invite-posts")
                        .header("Authorization", bearer("poster2@sfsu.edu"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"anyone who disagrees is an idiot\", \"inviteType\": \"SINGLE\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Please keep it friendly."));
    }

    @Test
    void aCleanInviteStillPostsNormally() throws Exception {
        user("poster3@sfsu.edu");

        mockMvc.perform(post("/api/invite-posts")
                        .header("Authorization", bearer("poster3@sfsu.edu"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"Coffee at the student center at 3?\", \"inviteType\": \"SINGLE\", \"tags\": [\"coffee\"]}"))
                .andExpect(status().isCreated());
    }

    @Test
    void unsafeBanterAndRepliesAreRejected() throws Exception {
        user("banterer@sfsu.edu");
        String token = bearer("banterer@sfsu.edu");

        mockMvc.perform(post("/api/banter")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"I will find you after class\"}"))
                .andExpect(status().isUnprocessableEntity());

        String created = mockMvc.perform(post("/api/banter")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"Best study spot on campus?\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String banterId = created.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(post("/api/banter/{id}/replies", banterId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"send me nudes\"}"))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(get("/api/banter/{id}/replies", banterId).header("Authorization", token))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ---- helpers ----

    private org.springframework.test.web.servlet.RequestBuilder draft(String email, String text) throws Exception {
        return post("/api/ai/invite-draft")
                .header("Authorization", bearer(email))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\": \"" + text + "\"}");
    }

    private AppUser user(String email) {
        AppUser user = users.createUser(
                "Test",
                "User",
                email,
                passwordEncoder.encode("Password123!"),
                "+1415555" + Math.abs(email.hashCode() % 10000),
                LocalDate.of(2000, 1, 1)
        );
        user.markEmailVerified(VERIFIED_AT);
        return user;
    }

    private String bearer(String email) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "Password123!"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return "Bearer " + response.split("\"token\":\"")[1].split("\"")[0];
    }
}
