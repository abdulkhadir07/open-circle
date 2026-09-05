package com.opencircle.realtime;

import com.opencircle.user.AppUser;
import com.opencircle.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.MessagingException;

import java.security.Principal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebSocketPrincipalResolverTest {

    private final UserService userService = mock(UserService.class);
    private final WebSocketPrincipalResolver resolver = new WebSocketPrincipalResolver(userService);

    @Test
    void resolveReturnsUserForWebSocketPrincipal() {
        UUID userId = UUID.randomUUID();
        AppUser user = user("socket.user@example.com");

        when(userService.findById(userId)).thenReturn(Optional.of(user));

        assertThat(resolver.resolve(new WebSocketUserPrincipal(userId))).isEqualTo(user);
    }

    @Test
    void resolveRejectsMissingPrincipal() {
        assertThatThrownBy(() -> resolver.resolve(null))
                .isInstanceOf(MessagingException.class)
                .hasMessage("Authentication required");
    }

    @Test
    void resolveRejectsUnsupportedPrincipalType() {
        Principal principal = () -> UUID.randomUUID().toString();

        assertThatThrownBy(() -> resolver.resolve(principal))
                .isInstanceOf(MessagingException.class)
                .hasMessage("Authentication required");
    }

    @Test
    void resolveRejectsPrincipalWhenUserNoLongerExists() {
        UUID userId = UUID.randomUUID();

        when(userService.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolve(new WebSocketUserPrincipal(userId)))
                .isInstanceOf(MessagingException.class)
                .hasMessage("Authentication required");
    }

    private AppUser user(String email) {
        return new AppUser(
                "test_" + Math.abs(email.hashCode()),
                "Test",
                "User",
                email,
                "hashed-password",
                "+1415555" + Math.abs(email.hashCode() % 10000),
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );
    }
}