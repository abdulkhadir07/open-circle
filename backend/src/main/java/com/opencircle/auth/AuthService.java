package com.opencircle.auth;

import com.opencircle.passwordreset.PasswordResetService;
import com.opencircle.security.JwtService;
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

    AuthService(
            UserService userService,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            EmailVerificationService emailVerificationService,
            PasswordResetService passwordResetService
    ) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailVerificationService = emailVerificationService;
        this.passwordResetService = passwordResetService;
    }

    @Transactional
    AuthResponse signup(SignupRequest request) {
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

        // Return the auth token and user details for the newly created account.
        return createAuthResponse(user);
    }

    @Transactional(readOnly = true)
    AuthResponse login(LoginRequest request) {
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

        // Return the auth token and user details after successful login.
        return createAuthResponse(user);
    }

    @Transactional
    AuthResponse verifyEmail(VerifyEmailRequest request) {
        // Find the user account for the submitted email address.
        AppUser user = userService.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        // Verify the submitted email verification code for this user.
        emailVerificationService.verify(user, request.code());

        // Return the auth token and updated user details after email verification.
        return createAuthResponse(user);
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

    private AuthResponse createAuthResponse(AppUser user) {
        // Build the response with a JWT token and user details.
        return new AuthResponse(
                jwtService.generateToken(user),
                UserResponse.from(user)
        );
    }
}
