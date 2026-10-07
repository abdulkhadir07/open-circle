package com.opencircle.banter;

import com.opencircle.user.AppUser;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BanterTest {

    private static final Instant NOW = Instant.parse("2026-10-04T12:00:00Z");

    @Test
    void banterTrimsContentAndKeepsTheAuthor() {
        Banter banter = new Banter(user("jane@student.sfsu.edu"), "  Best study spot on campus?  ", NOW);

        assertThat(banter.getContent()).isEqualTo("Best study spot on campus?");
        assertThat(banter.getCampus()).isEqualTo("sfsu.edu");
        assertThat(banter.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void banterAllowsExactlyTheMaximumLength() {
        Banter banter = new Banter(user("jane@sfsu.edu"), "x".repeat(280), NOW);

        assertThat(banter.getContent()).hasSize(280);
    }

    @Test
    void banterRejectsBlankContent() {
        assertThatThrownBy(() -> new Banter(user("jane@sfsu.edu"), "   ", NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Content is required");
        assertThatThrownBy(() -> new Banter(user("jane@sfsu.edu"), null, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Content is required");
    }

    @Test
    void banterRejectsTooLongContentAfterTrimming() {
        assertThatThrownBy(() -> new Banter(user("jane@sfsu.edu"), "x".repeat(281), NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Content must not exceed 280 characters");

        // Surrounding whitespace doesn't count against the limit.
        assertThat(new Banter(user("jane@sfsu.edu"), " " + "x".repeat(280) + " ", NOW).getContent()).hasSize(280);
    }

    @Test
    void banterRequiresAnAuthorAndATime() {
        assertThatThrownBy(() -> new Banter(null, "hello", NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Author is required");
        assertThatThrownBy(() -> new Banter(user("jane@sfsu.edu"), "hello", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Created time is required");
    }

    @Test
    void replyUsesTheSameContentRules() {
        Banter banter = new Banter(user("jane@sfsu.edu"), "hello", NOW);

        BanterReply reply = new BanterReply(banter, user("sam@sfsu.edu"), "  agreed  ", NOW);

        assertThat(reply.getContent()).isEqualTo("agreed");
        assertThatThrownBy(() -> new BanterReply(banter, user("sam@sfsu.edu"), " ", NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Content is required");
    }

    @Test
    void sortDefaultsToNewAndRejectsUnknownValues() {
        assertThat(BanterSort.parse(null)).isEqualTo(BanterSort.NEW);
        assertThat(BanterSort.parse("")).isEqualTo(BanterSort.NEW);
        assertThat(BanterSort.parse("HOT")).isEqualTo(BanterSort.HOT);
        assertThatThrownBy(() -> BanterSort.parse("random"))
                .isInstanceOf(InvalidBanterRequestException.class);
    }

    private AppUser user(String email) {
        return new AppUser(
                email.substring(0, email.indexOf('@')).replace('.', '_') + "_1234",
                "Test",
                "User",
                email,
                "hashed-password",
                "+14155550123",
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );
    }
}
