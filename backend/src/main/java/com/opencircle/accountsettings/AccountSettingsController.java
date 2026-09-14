package com.opencircle.accountsettings;

import com.opencircle.security.CurrentUserProvider;
import com.opencircle.session.CurrentSessionProvider;
import com.opencircle.session.RefreshTokenCookieService;
import com.opencircle.session.SessionDetails;
import com.opencircle.session.SessionRevocationReason;
import com.opencircle.session.SessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/me")
class AccountSettingsController {

    private final AccountSettingsService accountSettingsService;
    private final SessionService sessionService;
    private final CurrentUserProvider currentUserProvider;
    private final CurrentSessionProvider currentSessionProvider;
    private final RefreshTokenCookieService refreshTokenCookies;

    AccountSettingsController(
            AccountSettingsService accountSettingsService,
            SessionService sessionService,
            CurrentUserProvider currentUserProvider,
            CurrentSessionProvider currentSessionProvider,
            RefreshTokenCookieService refreshTokenCookies
    ) {
        this.accountSettingsService = accountSettingsService;
        this.sessionService = sessionService;
        this.currentUserProvider = currentUserProvider;
        this.currentSessionProvider = currentSessionProvider;
        this.refreshTokenCookies = refreshTokenCookies;
    }

    @PutMapping("/password")
    ResponseEntity<AccountAccessTokenResponse> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        AccountSecurityUpdate update = accountSettingsService.changePassword(
                currentUserProvider.getCurrentUserId(jwt),
                currentSessionProvider.getCurrentSessionId(jwt),
                request.currentPassword(),
                request.newPassword()
        );
        return securityUpdateResponse(update);
    }

    @PostMapping("/email-change")
    ResponseEntity<Void> requestEmailChange(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody StartEmailChangeRequest request
    ) {
        accountSettingsService.requestEmailChange(
                currentUserProvider.getCurrentUserId(jwt),
                request.newEmail(),
                request.currentPassword()
        );
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/email-change/verify")
    ResponseEntity<AccountAccessTokenResponse> verifyEmailChange(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody VerifyEmailChangeRequest request
    ) {
        AccountSecurityUpdate update = accountSettingsService.verifyEmailChange(
                currentUserProvider.getCurrentUserId(jwt),
                currentSessionProvider.getCurrentSessionId(jwt),
                request.code()
        );
        return securityUpdateResponse(update);
    }

    @GetMapping("/sessions")
    AccountSessionsResponse getSessions(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = currentUserProvider.getCurrentUserId(jwt);
        UUID currentSessionId = currentSessionProvider.getCurrentSessionId(jwt);
        List<SessionDetails> sessions = sessionService.getActiveSessions(userId, currentSessionId);
        return new AccountSessionsResponse(sessions);
    }

    @DeleteMapping("/sessions/{sessionId}")
    ResponseEntity<Void> revokeSession(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID sessionId
    ) {
        UUID userId = currentUserProvider.getCurrentUserId(jwt);
        UUID currentSessionId = currentSessionProvider.getCurrentSessionId(jwt);
        sessionService.revokeOwned(userId, sessionId, SessionRevocationReason.USER_REVOKED);

        ResponseEntity.HeadersBuilder<?> response = ResponseEntity.noContent();
        if (sessionId.equals(currentSessionId)) {
            response.header(HttpHeaders.SET_COOKIE, refreshTokenCookies.clear().toString());
        }
        return response.build();
    }

    @DeleteMapping("/sessions")
    ResponseEntity<Void> revokeAllSessions(@AuthenticationPrincipal Jwt jwt) {
        sessionService.revokeAll(
                currentUserProvider.getCurrentUserId(jwt),
                SessionRevocationReason.LOGOUT_ALL
        );
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookies.clear().toString())
                .build();
    }

    private ResponseEntity<AccountAccessTokenResponse> securityUpdateResponse(AccountSecurityUpdate update) {
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshTokenCookies.create(
                                update.session().refreshToken(),
                                update.session().refreshTokenExpiresAt()
                        ).toString()
                )
                .body(new AccountAccessTokenResponse(update.accessToken()));
    }
}
