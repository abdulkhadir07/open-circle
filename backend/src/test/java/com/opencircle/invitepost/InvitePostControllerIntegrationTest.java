package com.opencircle.invitepost;

import com.opencircle.AbstractIntegrationTest;
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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InvitePostControllerIntegrationTest extends AbstractIntegrationTest {

    private static final Instant VERIFIED_AT = Instant.parse("2026-08-30T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private InvitePostRepository posts;

    @Test
    void createPostReturnsCreatedInvitePost() throws Exception {
        AppUser user = verifiedUser("poster@example.com", "San Francisco", "California", "USA");
        String token = loginToken("poster@example.com");

        mockMvc.perform(post("/api/invite-posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "Anyone want to grab coffee near campus?",
                                  "inviteType": "GROUP",
                                  "totalCapacity": 4,
                                  "locationScope": "CITY"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.posterId").value(user.getId().toString()))
                .andExpect(jsonPath("$.posterUsername").value(user.getUsername()))
                .andExpect(jsonPath("$.content").value("Anyone want to grab coffee near campus?"))
                .andExpect(jsonPath("$.inviteType").value("GROUP"))
                .andExpect(jsonPath("$.totalCapacity").value(4))
                .andExpect(jsonPath("$.acceptedCount").value(0))
                .andExpect(jsonPath("$.invitesLeft").value(4))
                .andExpect(jsonPath("$.locationScope").value("CITY"))
                .andExpect(jsonPath("$.city").value("San Francisco"))
                .andExpect(jsonPath("$.stateRegion").value("California"))
                .andExpect(jsonPath("$.country").value("USA"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createPostReturnsForbiddenWhenLocationIsNotVerified() throws Exception {
        AppUser user = users.createUser(
                "Jane",
                "Doe",
                "unverified.location@example.com",
                passwordEncoder.encode("Password123!"),
                "+14155550110",
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );
        user.markEmailVerified(VERIFIED_AT);

        String token = loginToken("unverified.location@example.com");

        mockMvc.perform(post("/api/invite-posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "This should not post yet",
                                  "inviteType": "SINGLE",
                                  "totalCapacity": 1,
                                  "locationScope": "GLOBAL"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Please verify your location before using this feature"));
    }

    @Test
    void localFeedReturnsMatchingLocalPostsOnly() throws Exception {
        Instant feedNow = Instant.now();

        verifiedUser("viewer@example.com", "San Francisco", "California", "USA");
        AppUser localPoster = verifiedUser("local.poster@example.com", "San Francisco", "California", "USA");
        AppUser globalPoster = verifiedUser("global.poster@example.com", "San Francisco", "California", "USA");
        AppUser otherCountryPoster = verifiedUser("other.country@example.com", "Toronto", "Ontario", "Canada");

        posts.save(invitePost(localPoster, "City post", LocationScope.CITY, "San Francisco", "California", "USA", feedNow.minusSeconds(60)));
        posts.save(invitePost(localPoster, "Country post", LocationScope.COUNTRY, "Los Angeles", "California", "USA", feedNow.minusSeconds(120)));
        posts.save(invitePost(globalPoster, "Global post", LocationScope.GLOBAL, "San Francisco", "California", "USA", feedNow.minusSeconds(180)));
        posts.save(invitePost(otherCountryPoster, "Canada post", LocationScope.COUNTRY, "Toronto", "Ontario", "Canada", feedNow.minusSeconds(240)));

        String token = loginToken("viewer@example.com");

        mockMvc.perform(get("/api/invite-posts/local")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].content").value("City post"))
                .andExpect(jsonPath("$[1].content").value("Country post"));
    }

    @Test
    void globalFeedReturnsGlobalPostsOnly() throws Exception {
        Instant feedNow = Instant.now();

        verifiedUser("global.viewer@example.com", "San Francisco", "California", "USA");
        AppUser poster = verifiedUser("global.feed.poster@example.com", "Banjul", null, "The Gambia");

        posts.save(invitePost(poster, "Global invite", LocationScope.GLOBAL, "Banjul", null, "The Gambia", feedNow.minusSeconds(60)));
        posts.save(invitePost(poster, "Country invite", LocationScope.COUNTRY, "Banjul", null, "The Gambia", feedNow.minusSeconds(120)));

        String token = loginToken("global.viewer@example.com");

        mockMvc.perform(get("/api/invite-posts/global")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].content").value("Global invite"));
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
                "+1415555" + Math.abs(email.hashCode() % 10000),
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
            LocationScope scope,
            String city,
            String stateRegion,
            String country,
            Instant createdAt
    ) {
        return new InvitePost(
                poster,
                content,
                InviteType.GROUP,
                3,
                scope,
                city,
                stateRegion,
                country,
                createdAt
        );
    }
}