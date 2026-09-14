package com.opencircle.accountsettings;

import com.opencircle.AbstractIntegrationTest;
import com.opencircle.common.OtpCodeGenerator;
import com.opencircle.mail.MailService;
import com.opencircle.session.IssuedSession;
import com.opencircle.session.SessionService;
import com.opencircle.user.AppUser;
import com.opencircle.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class AccountSettingsConcurrencyIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "Password123!";
    private static final String SHARED_CODE = "123456";
    private static final AtomicInteger PHONE_SEQUENCE = new AtomicInteger(7_000_000);

    @Autowired private AccountSettingsService accountSettingsService;
    @Autowired private UserService userService;
    @Autowired private SessionService sessionService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbcTemplate;

    @MockitoBean private OtpCodeGenerator codeGenerator;
    @MockitoBean private MailService mailService;

    @BeforeEach
    void setUpCode() {
        when(codeGenerator.generate()).thenReturn(SHARED_CODE);
    }

    @Test
    void concurrentVerificationAllowsOnlyOneAccountToClaimAnEmail() throws Exception {
        TestAccount first = createAccount("email-race-first");
        TestAccount second = createAccount("email-race-second");
        String targetEmail = "claimed-once@example.com";
        accountSettingsService.requestEmailChange(first.user().getId(), targetEmail, PASSWORD);
        accountSettingsService.requestEmailChange(second.user().getId(), targetEmail, PASSWORD);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<VerificationOutcome> firstResult = executor.submit(
                    () -> verifyWhenReleased(first, ready, start)
            );
            Future<VerificationOutcome> secondResult = executor.submit(
                    () -> verifyWhenReleased(second, ready, start)
            );

            ready.await();
            start.countDown();
            List<VerificationOutcome> outcomes = List.of(firstResult.get(), secondResult.get());

            assertThat(outcomes).filteredOn(VerificationOutcome::succeeded).hasSize(1);
            assertThat(outcomes).filteredOn(outcome -> !outcome.succeeded())
                    .singleElement()
                    .satisfies(outcome -> assertThat(outcome.failureType())
                            .isEqualTo(EmailAlreadyInUseException.class));

            Integer owners = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users WHERE LOWER(email) = LOWER(?)",
                    Integer.class,
                    targetEmail
            );
            assertThat(owners).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private VerificationOutcome verifyWhenReleased(
            TestAccount account,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();
        start.await();

        try {
            accountSettingsService.verifyEmailChange(
                    account.user().getId(),
                    account.session().sessionId(),
                    SHARED_CODE
            );
            return new VerificationOutcome(true, null);
        } catch (RuntimeException exception) {
            return new VerificationOutcome(false, exception.getClass());
        }
    }

    private TestAccount createAccount(String suffix) {
        AppUser user = userService.createUser(
                "Settings",
                "Tester",
                suffix + "@example.com",
                passwordEncoder.encode(PASSWORD),
                "+1415" + PHONE_SEQUENCE.incrementAndGet(),
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );
        user.markEmailVerified(Instant.now());
        userService.save(user);
        return new TestAccount(user, sessionService.create(user, "Concurrency Browser"));
    }

    private record TestAccount(AppUser user, IssuedSession session) {
    }

    private record VerificationOutcome(boolean succeeded, Class<?> failureType) {
    }
}
