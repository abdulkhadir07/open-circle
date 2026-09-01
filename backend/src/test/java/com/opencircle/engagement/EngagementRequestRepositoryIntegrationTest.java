package com.opencircle.engagement;

import com.opencircle.AbstractIntegrationTest;
import com.opencircle.invitepost.InvitePost;
import com.opencircle.invitepost.InviteType;
import com.opencircle.invitepost.LocationScope;
import com.opencircle.user.AppUser;
import com.opencircle.user.UserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EngagementRequestRepositoryIntegrationTest extends AbstractIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-08-30T12:00:00Z");

    @Autowired
    private EngagementRequestRepository requests;

    @Autowired
    private UserService users;

    @Autowired
    private EntityManager entityManager;

    @Test
    void existsByInvitePostAndRequesterReturnsTrueWhenRequestExists() {
        AppUser poster = user("poster.exists@example.com");
        AppUser requester = user("requester.exists@example.com");
        InvitePost post = invitePost(poster);

        requests.save(new EngagementRequest(post, requester, NOW.minusSeconds(60)));

        assertThat(requests.existsByInvitePostAndRequester(post, requester)).isTrue();
    }

    @Test
    void findByInvitePostAndRequesterReturnsMatchingRequest() {
        AppUser poster = user("poster.find@example.com");
        AppUser requester = user("requester.find@example.com");
        InvitePost post = invitePost(poster);

        EngagementRequest savedRequest = requests.save(new EngagementRequest(post, requester, NOW.minusSeconds(60)));

        assertThat(requests.findByInvitePostAndRequester(post, requester))
                .contains(savedRequest);
    }

    @Test
    void findByInvitePostOrderByCreatedAtDescReturnsNewestFirst() {
        AppUser poster = user("poster.order@example.com");
        AppUser firstRequester = user("first.requester@example.com");
        AppUser secondRequester = user("second.requester@example.com");
        InvitePost post = invitePost(poster);

        EngagementRequest olderRequest = requests.save(new EngagementRequest(post, firstRequester, NOW.minusSeconds(120)));
        EngagementRequest newerRequest = requests.save(new EngagementRequest(post, secondRequester, NOW.minusSeconds(60)));

        assertThat(requests.findByInvitePostOrderByCreatedAtDesc(post))
                .containsExactly(newerRequest, olderRequest);
    }

    @Test
    void uniqueConstraintPreventsDuplicateRequesterForSamePost() {
        AppUser poster = user("poster.duplicate@example.com");
        AppUser requester = user("requester.duplicate@example.com");
        InvitePost post = invitePost(poster);

        requests.saveAndFlush(new EngagementRequest(post, requester, NOW.minusSeconds(120)));

        assertThatThrownBy(() -> requests.saveAndFlush(new EngagementRequest(post, requester, NOW.minusSeconds(60))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findDetailedByIdLoadsRequestPostPosterAndRequester() {
        AppUser poster = user("poster.detail@example.com");
        AppUser requester = user("requester.detail@example.com");
        InvitePost post = invitePost(poster);

        EngagementRequest savedRequest = requests.saveAndFlush(new EngagementRequest(post, requester, NOW.minusSeconds(60)));

        entityManager.clear();

        EngagementRequest foundRequest = requests.findDetailedById(savedRequest.getId()).orElseThrow();

        assertThat(foundRequest.getInvitePost().getContent()).isEqualTo("Anyone want to hang out?");
        assertThat(foundRequest.getInvitePost().getPoster().getEmail()).isEqualTo("poster.detail@example.com");
        assertThat(foundRequest.getRequester().getEmail()).isEqualTo("requester.detail@example.com");
    }

    private AppUser user(String email) {
        return users.createUser(
                "Test",
                "User",
                email,
                "hashed-password",
                phoneNumber(email),
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );
    }

    private InvitePost invitePost(AppUser poster) {
        InvitePost post = new InvitePost(
                poster,
                "Anyone want to hang out?",
                InviteType.GROUP,
                3,
                LocationScope.CITY,
                "San Francisco",
                "California",
                "USA",
                NOW
        );

        entityManager.persist(post);
        entityManager.flush();

        return post;
    }

    private String phoneNumber(String email) {
        long suffix = Integer.toUnsignedLong(email.hashCode()) % 10_000_000_000L;
        return "+1%010d".formatted(suffix);
    }
}