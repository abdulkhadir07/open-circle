package com.opencircle.banter;

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
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BanterControllerIntegrationTest extends AbstractIntegrationTest {

    private static final Instant VERIFIED_AT = Instant.parse("2026-08-30T00:00:00Z");
    private static final Instant BASE = Instant.parse("2026-10-01T12:00:00Z");

    @Autowired private MockMvc mockMvc;
    @Autowired private UserService users;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private BanterRepository banters;
    @Autowired private BanterReplyRepository replies;

    @MockitoBean private ProfileImageQueryService profileImageQueryService;

    @BeforeEach
    void defaultMissingProfileImages() {
        when(profileImageQueryService.getProfileImagesByUserIds(any())).thenReturn(Map.of());
    }

    @Test
    void createBanterReturnsTheNewPostForTheAuthorsCampus() throws Exception {
        AppUser author = user("author@student.sfsu.edu");

        mockMvc.perform(postBanter("author@student.sfsu.edu", "  Best study spot on campus?  "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorId").value(author.getId().toString()))
                .andExpect(jsonPath("$.authorUsername").value(author.getUsername()))
                .andExpect(jsonPath("$.content").value("Best study spot on campus?"))
                .andExpect(jsonPath("$.likeCount").value(0))
                .andExpect(jsonPath("$.replyCount").value(0))
                .andExpect(jsonPath("$.likedByMe").value(false))
                .andExpect(jsonPath("$.mine").value(true));
    }

    @Test
    void createBanterRejectsBlankAndTooLongContent() throws Exception {
        user("author@sfsu.edu");

        mockMvc.perform(postBanter("author@sfsu.edu", "   "))
                .andExpect(status().isBadRequest());
        mockMvc.perform(postBanter("author@sfsu.edu", "x".repeat(281)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.content").value("Content must not exceed 280 characters"));
    }

    @Test
    void boardListsOnlyTheViewersCampusNewestFirst() throws Exception {
        AppUser viewer = user("viewer@student.sfsu.edu");
        AppUser poster = user("poster@mail.sfsu.edu");
        AppUser stranger = user("stranger@stanford.edu");

        banters.save(new Banter(poster, "older", BASE));
        banters.save(new Banter(viewer, "newer", BASE.plusSeconds(60)));
        banters.save(new Banter(stranger, "other campus", BASE.plusSeconds(120)));

        mockMvc.perform(getBoard("viewer@student.sfsu.edu", "new", 0, 20))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].content").value("newer"))
                .andExpect(jsonPath("$.items[0].mine").value(true))
                .andExpect(jsonPath("$.items[1].content").value("older"))
                .andExpect(jsonPath("$.items[1].mine").value(false))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void boardPagesResults() throws Exception {
        AppUser viewer = user("viewer@sfsu.edu");
        for (int index = 0; index < 5; index++) {
            banters.save(new Banter(viewer, "post " + index, BASE.plusSeconds(index)));
        }

        mockMvc.perform(getBoard("viewer@sfsu.edu", "new", 0, 2))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].content").value("post 4"))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));

        mockMvc.perform(getBoard("viewer@sfsu.edu", "new", 2, 2))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].content").value("post 0"));
    }

    @Test
    void boardRejectsBadPagingAndSortParameters() throws Exception {
        user("viewer@sfsu.edu");

        mockMvc.perform(getBoard("viewer@sfsu.edu", "new", -1, 20)).andExpect(status().isBadRequest());
        mockMvc.perform(getBoard("viewer@sfsu.edu", "new", 0, 0)).andExpect(status().isBadRequest());
        mockMvc.perform(getBoard("viewer@sfsu.edu", "new", 0, 101)).andExpect(status().isBadRequest());
        mockMvc.perform(getBoard("viewer@sfsu.edu", "random", 0, 20)).andExpect(status().isBadRequest());
    }

    @Test
    void hotSortWeighsRepliesAsTwoLikesAndBreaksTiesByNewest() throws Exception {
        AppUser viewer = user("viewer@sfsu.edu");
        AppUser fan1 = user("fan1@sfsu.edu");
        AppUser fan2 = user("fan2@sfsu.edu");
        AppUser fan3 = user("fan3@sfsu.edu");

        Banter quiet = banters.save(new Banter(viewer, "quiet", BASE.plusSeconds(300)));
        Banter liked = banters.save(new Banter(viewer, "liked", BASE));
        Banter discussed = banters.save(new Banter(viewer, "discussed", BASE.plusSeconds(10)));
        Banter tiedNewer = banters.save(new Banter(viewer, "tied newer", BASE.plusSeconds(200)));

        // liked: 2 likes = 2. discussed: 1 reply = 2 (ties liked, but is newer). tiedNewer: 1 like = 1. quiet: 0.
        likeVia("fan1@sfsu.edu", liked);
        likeVia("fan2@sfsu.edu", liked);
        replies.save(new BanterReply(discussed, fan3, "interesting", BASE.plusSeconds(20)));
        likeVia("fan1@sfsu.edu", tiedNewer);

        mockMvc.perform(getBoard("viewer@sfsu.edu", "hot", 0, 20))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].content").value("discussed"))
                .andExpect(jsonPath("$.items[1].content").value("liked"))
                .andExpect(jsonPath("$.items[2].content").value("tied newer"))
                .andExpect(jsonPath("$.items[3].content").value("quiet"))
                .andExpect(jsonPath("$.items[0].replyCount").value(1))
                .andExpect(jsonPath("$.items[1].likeCount").value(2));
    }

    @Test
    void likingIsIdempotentAndTheBoardReportsLikedByMe() throws Exception {
        AppUser author = user("author@sfsu.edu");
        user("fan@sfsu.edu");
        Banter banter = banters.save(new Banter(author, "like me", BASE));

        mockMvc.perform(put("/api/banter/{id}/like", banter.getId()).header("Authorization", bearer("fan@sfsu.edu")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(1))
                .andExpect(jsonPath("$.likedByMe").value(true));
        // A second like (or two racing taps) doesn't double count.
        mockMvc.perform(put("/api/banter/{id}/like", banter.getId()).header("Authorization", bearer("fan@sfsu.edu")))
                .andExpect(jsonPath("$.likeCount").value(1));

        mockMvc.perform(getBoard("fan@sfsu.edu", "new", 0, 20))
                .andExpect(jsonPath("$.items[0].likeCount").value(1))
                .andExpect(jsonPath("$.items[0].likedByMe").value(true));
        mockMvc.perform(getBoard("author@sfsu.edu", "new", 0, 20))
                .andExpect(jsonPath("$.items[0].likedByMe").value(false));

        mockMvc.perform(delete("/api/banter/{id}/like", banter.getId()).header("Authorization", bearer("fan@sfsu.edu")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(0))
                .andExpect(jsonPath("$.likedByMe").value(false));
        mockMvc.perform(delete("/api/banter/{id}/like", banter.getId()).header("Authorization", bearer("fan@sfsu.edu")))
                .andExpect(jsonPath("$.likeCount").value(0));
    }

    @Test
    void repliesAreListedOldestFirstAndCountedOnTheBoard() throws Exception {
        AppUser author = user("author@sfsu.edu");
        user("replier@sfsu.edu");
        Banter banter = banters.save(new Banter(author, "start a thread", BASE));

        mockMvc.perform(post("/api/banter/{id}/replies", banter.getId())
                        .header("Authorization", bearer("replier@sfsu.edu"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"first!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("first!"))
                .andExpect(jsonPath("$.mine").value(true));
        mockMvc.perform(post("/api/banter/{id}/replies", banter.getId())
                        .header("Authorization", bearer("author@sfsu.edu"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"second\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/banter/{id}/replies", banter.getId()).header("Authorization", bearer("author@sfsu.edu")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].content").value("first!"))
                .andExpect(jsonPath("$[0].mine").value(false))
                .andExpect(jsonPath("$[1].content").value("second"))
                .andExpect(jsonPath("$[1].mine").value(true));

        mockMvc.perform(getBoard("author@sfsu.edu", "new", 0, 20))
                .andExpect(jsonPath("$.items[0].replyCount").value(2));
    }

    @Test
    void replyRejectsBlankContent() throws Exception {
        AppUser author = user("author@sfsu.edu");
        Banter banter = banters.save(new Banter(author, "thread", BASE));

        mockMvc.perform(post("/api/banter/{id}/replies", banter.getId())
                        .header("Authorization", bearer("author@sfsu.edu"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void onlyTheAuthorCanDeleteTheirBanter() throws Exception {
        AppUser author = user("author@sfsu.edu");
        user("other@sfsu.edu");
        Banter banter = banters.save(new Banter(author, "mine", BASE));

        mockMvc.perform(delete("/api/banter/{id}", banter.getId()).header("Authorization", bearer("other@sfsu.edu")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only delete your own banter"));

        mockMvc.perform(delete("/api/banter/{id}", banter.getId()).header("Authorization", bearer("author@sfsu.edu")))
                .andExpect(status().isNoContent());

        mockMvc.perform(getBoard("author@sfsu.edu", "new", 0, 20))
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void onlyTheAuthorCanDeleteTheirReply() throws Exception {
        AppUser author = user("author@sfsu.edu");
        AppUser replier = user("replier@sfsu.edu");
        Banter banter = banters.save(new Banter(author, "thread", BASE));
        BanterReply reply = replies.save(new BanterReply(banter, replier, "my reply", BASE.plusSeconds(5)));

        mockMvc.perform(delete("/api/banter/{id}/replies/{replyId}", banter.getId(), reply.getId())
                        .header("Authorization", bearer("author@sfsu.edu")))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/banter/{id}/replies/{replyId}", banter.getId(), reply.getId())
                        .header("Authorization", bearer("replier@sfsu.edu")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/banter/{id}/replies", banter.getId()).header("Authorization", bearer("author@sfsu.edu")))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void banterFromAnotherCampusLooksLikeItDoesNotExist() throws Exception {
        AppUser author = user("author@sfsu.edu");
        user("outsider@stanford.edu");
        Banter banter = banters.save(new Banter(author, "sfsu only", BASE));
        String outsider = bearer("outsider@stanford.edu");

        mockMvc.perform(put("/api/banter/{id}/like", banter.getId()).header("Authorization", outsider))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/banter/{id}/replies", banter.getId()).header("Authorization", outsider))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/banter/{id}/replies", banter.getId())
                        .header("Authorization", outsider)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"hi\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/banter/{id}", banter.getId()).header("Authorization", outsider))
                .andExpect(status().isNotFound());
        mockMvc.perform(getBoard("outsider@stanford.edu", "new", 0, 20))
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void unknownBanterReturnsNotFound() throws Exception {
        user("viewer@sfsu.edu");

        mockMvc.perform(put("/api/banter/{id}/like", UUID.randomUUID()).header("Authorization", bearer("viewer@sfsu.edu")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Banter not found"));
    }

    @Test
    void banterEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/banter")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/banter").contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"hi\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/banter/{id}/like", UUID.randomUUID())).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/banter/{id}", UUID.randomUUID())).andExpect(status().isUnauthorized());
    }

    private void likeVia(String email, Banter banter) throws Exception {
        mockMvc.perform(put("/api/banter/{id}/like", banter.getId()).header("Authorization", bearer(email)))
                .andExpect(status().isOk());
    }

    private RequestBuilder getBoard(String email, String sort, int page, int size) throws Exception {
        return get("/api/banter")
                .header("Authorization", bearer(email))
                .param("sort", sort)
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size));
    }

    private RequestBuilder postBanter(String email, String content) throws Exception {
        return post("/api/banter")
                .header("Authorization", bearer(email))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\": \"" + content.replace("\"", "\\\"") + "\"}");
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
