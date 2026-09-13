package com.opencircle.auth;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import com.opencircle.session.InvalidRefreshTokenException;
import com.opencircle.session.RefreshTokenCookieService;
import com.opencircle.session.SessionOriginValidator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieService refreshTokenCookies;
    private final SessionOriginValidator sessionOriginValidator;

    AuthController(
            AuthService authService,
            RefreshTokenCookieService refreshTokenCookies,
            SessionOriginValidator sessionOriginValidator
    ) {
        this.authService = authService;
        this.refreshTokenCookies = refreshTokenCookies;
        this.sessionOriginValidator = sessionOriginValidator;
    }

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent
    ) {
        return authenticatedResponse(authService.login(request, userAgent));
    }

    @PostMapping("/verify-email")
    ResponseEntity<AuthResponse> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent
    ) {
        return authenticatedResponse(authService.verifyEmail(request, userAgent));
    }

    @PostMapping("/resend-verification")
    ResponseEntity<Void> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        authService.resendVerification(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    ResponseEntity<AccessTokenResponse> refresh(HttpServletRequest request) {
        sessionOriginValidator.validate(request);
        String rawRefreshToken = refreshTokenCookies.read(request)
                .orElseThrow(InvalidRefreshTokenException::new);

        RefreshedAuthentication refreshed = authService.refresh(rawRefreshToken);
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshTokenCookies.create(
                                refreshed.refreshToken(),
                                refreshed.refreshTokenExpiresAt()
                        ).toString()
                )
                .body(refreshed.response());
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(HttpServletRequest request) {
        sessionOriginValidator.validate(request);
        refreshTokenCookies.read(request).ifPresent(authService::logout);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookies.clear().toString())
                .build();
    }

    private ResponseEntity<AuthResponse> authenticatedResponse(AuthenticatedSession authenticated) {
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshTokenCookies.create(
                                authenticated.session().refreshToken(),
                                authenticated.session().refreshTokenExpiresAt()
                        ).toString()
                )
                .body(authenticated.response());
    }
}
