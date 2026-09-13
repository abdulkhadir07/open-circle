package com.opencircle.auth;

import com.opencircle.passwordreset.PasswordResetService;
import com.opencircle.profileimage.ProfileImageQueryService;
import com.opencircle.security.JwtService;
import com.opencircle.session.IssuedSession;
import com.opencircle.session.RefreshedSession;
import com.opencircle.session.SessionService;
import com.opencircle.user.AppUser;
import com.opencircle.user.UserService;
import com.opencircle.user.dto.UserResponse;
import com.opencircle.verification.EmailVerificationService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;
    private final ProfileImageQueryService profileImageQueryService;
    private final SessionService sessionService;

    AuthService(
            UserService userService,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            EmailVerificationService emailVerificationService,
            PasswordResetService passwordResetService,
            ProfileImageQueryService profileImageQueryService,
            SessionService sessionService
    ) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailVerificationService = emailVerificationService;
        this.passwordResetService = passwordResetService;
        this.profileImageQueryService = profileImageQueryService;
        this.sessionService = sessionService;
    }

    @Transactional
    SignupResponse signup(SignupRequest request) {
        // Check if the submitted email already belongs to an existing user.
        if (userService.emailExists(request.email())) {
            throw new EmailAlreadyExistsException();
        }

        // Check if the submitted phone number already belongs to an existing user.
        if (userService.phoneNumberExists(request.phoneNumber())) {
            throw new PhoneNumberAlreadyExistsException();
        }

        // Create the user with a hashed password.
        AppUser user = userService.createUser(
                request.firstName(),
                request.lastName(),
                request.email(),
                passwordEncoder.encode(request.password()),
                request.phoneNumber(),
                request.dateOfBirth(),
                request.city(),
                request.stateRegion(),
                request.country()
        );

        // Send the email verification code after the user is created.
        emailVerificationService.issueCode(user);

        // Unverified accounts never receive access or refresh credentials.
        return new SignupResponse(UserResponse.from(user, null));
    }

    @Transactional
    AuthenticatedSession login(LoginRequest request, String userAgent) {
        // Find the user account for the submitted email address.
        AppUser user = userService.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        // Compare the submitted password with the stored hashed password.
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        // Stop login until the user has verified their email address.
        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException();
        }

        return createAuthenticatedSession(user, userAgent);
    }

    @Transactional
    AuthenticatedSession verifyEmail(VerifyEmailRequest request, String userAgent) {
        // Find the user account for the submitted email address.
        AppUser user = userService.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        // Verify the submitted email verification code for this user.
        emailVerificationService.verify(user, request.code());

        return createAuthenticatedSession(user, userAgent);
    }

    @Transactional
    void resendVerification(ResendVerificationRequest request) {
        // Find the user account for the submitted email address.
        AppUser user = userService.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        // Do not send another verification code when the email is already verified.
        if (user.isEmailVerified()) {
            return;
        }

        // Send a new email verification code to the user.
        emailVerificationService.issueCode(user);
    }

    @Transactional
    void forgotPassword(ForgotPasswordRequest request) {
        // Start the password reset flow for the submitted email address.
        passwordResetService.requestReset(request.email());
    }

    @Transactional
    void resetPassword(ResetPasswordRequest request) {
        // Reset the user's password using the submitted email, code, and new password.
        passwordResetService.resetPassword(
                request.email(),
                request.code(),
                request.newPassword()
        );
    }

    RefreshedAuthentication refresh(String rawRefreshToken) {
        RefreshedSession refreshed = sessionService.refresh(rawRefreshToken);
        AppUser user = userService.getById(refreshed.userId());
        return new RefreshedAuthentication(
                new AccessTokenResponse(jwtService.generateToken(user)),
                refreshed.refreshToken(),
                refreshed.refreshTokenExpiresAt()
        );
    }

    @Transactional
    void logout(String rawRefreshToken) {
        sessionService.revokeCurrent(rawRefreshToken);
    }

    private AuthenticatedSession createAuthenticatedSession(AppUser user, String userAgent) {
        IssuedSession session = sessionService.create(user, userAgent);
        return new AuthenticatedSession(createAuthResponse(user), session);
    }

    private AuthResponse createAuthResponse(AppUser user) {
        return new AuthResponse(
                jwtService.generateToken(user),
                UserResponse.from(
                        user,
                        profileImageQueryService.getProfileImageByUserId(user.getId())
                )
        );
    }
}
