package com.opencircle.auth;

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

    AuthService(
            UserService userService,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            EmailVerificationService emailVerificationService
    ) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailVerificationService = emailVerificationService;
    }

    @Transactional
    AuthResponse signup(SignupRequest request) {
        if (userService.emailExists(request.email())) {
            throw new EmailAlreadyExistsException();
        }

        if (userService.phoneNumberExists(request.phoneNumber())) {
            throw new PhoneNumberAlreadyExistsException();
        }

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

        emailVerificationService.issueCode(user);

        return createAuthResponse(user);
    }

    @Transactional(readOnly = true)
    AuthResponse login(LoginRequest request) {
        AppUser user = userService.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException();
        }

        return createAuthResponse(user);
    }

    @Transactional
    AuthResponse verifyEmail(VerifyEmailRequest request) {
        AppUser user = userService.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        emailVerificationService.verify(user, request.code());

        return createAuthResponse(user);
    }

    @Transactional
    void resendVerification(ResendVerificationRequest request) {
        AppUser user = userService.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (user.isEmailVerified()) {
            return;
        }

        emailVerificationService.issueCode(user);
    }

    private AuthResponse createAuthResponse(AppUser user) {
        return new AuthResponse(
                jwtService.generateToken(user),
                UserResponse.from(user)
        );
    }
}
