package com.opencircle.user;

import com.opencircle.profileimage.ProfileImageQueryService;
import com.opencircle.profileimage.ProfileImageResponse;
import com.opencircle.rating.LifetimeReputationSummary;
import com.opencircle.rating.ReputationSummaryQueryService;
import com.opencircle.score.AnnualAwardHistoryQueryService;
import com.opencircle.score.EarnedAnnualAward;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserProfileServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-12T14:00:00Z");

    private final UserProfileRepository profiles = mock(UserProfileRepository.class);
    private final ProfileImageQueryService profileImages = mock(ProfileImageQueryService.class);
    private final ReputationSummaryQueryService reputationSummaries = mock(ReputationSummaryQueryService.class);
    private final AnnualAwardHistoryQueryService awardHistory = mock(AnnualAwardHistoryQueryService.class);
    private final UserProfileService service = new UserProfileService(
            profiles,
            profileImages,
            reputationSummaries,
            awardHistory,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void assemblesOnePublicProfileFromTheOwnedQueryServices() {
        UUID userId = UUID.randomUUID();
        UUID imageId = UUID.randomUUID();
        Instant memberSince = Instant.parse("2025-05-01T12:00:00Z");
        AppUser user = mock(AppUser.class);
        UserProfile profile = mock(UserProfile.class);
        ProfileImageResponse image = new ProfileImageResponse(
                imageId,
                "https://example.com/profile.jpg",
                NOW.plusSeconds(3600),
                "image/jpeg",
                NOW
        );
        LifetimeReputationSummary reputation = new LifetimeReputationSummary(
                new BigDecimal("4.75"),
                6,
                4
        );
        List<EarnedAnnualAward> awards = List.of(
                new EarnedAnnualAward(2025, 80, Instant.parse("2026-01-01T00:05:00Z")),
                new EarnedAnnualAward(2024, 65, Instant.parse("2025-01-01T00:05:00Z"))
        );

        when(profiles.findForDisplay(userId)).thenReturn(Optional.of(profile));
        when(profile.getUser()).thenReturn(user);
        when(profile.getDisplayName()).thenReturn("Open Explorer");
        when(profile.getBio()).thenReturn("Coffee, walks, and good conversation.");
        when(profile.getInterests()).thenReturn(List.of("Coffee", "Hiking"));
        when(user.getId()).thenReturn(userId);
        when(user.getUsername()).thenReturn("open_explorer_1234");
        when(user.getCreatedAt()).thenReturn(memberSince);
        when(profileImages.getProfileImageByUserId(userId)).thenReturn(image);
        when(reputationSummaries.getLifetimeSummary(userId)).thenReturn(reputation);
        when(awardHistory.findByWinnerUserId(userId)).thenReturn(awards);

        UserProfileResponse result = service.getProfile(userId);

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.username()).isEqualTo("open_explorer_1234");
        assertThat(result.displayName()).isEqualTo("Open Explorer");
        assertThat(result.profileImage()).isEqualTo(image);
        assertThat(result.bio()).isEqualTo("Coffee, walks, and good conversation.");
        assertThat(result.interests()).containsExactly("Coffee", "Hiking");
        assertThat(result.memberSince()).isEqualTo(memberSince);
        assertThat(result.reputation()).isEqualTo(new ProfileReputationResponse(
                new BigDecimal("4.75"),
                6,
                4
        ));
        assertThat(result.awards())
                .extracting(UserProfileAwardResponse::seasonYear)
                .containsExactly(2025, 2024);
        verify(profileImages).getProfileImageByUserId(userId);
        verify(reputationSummaries).getLifetimeSummary(userId);
        verify(awardHistory).findByWinnerUserId(userId);
    }

    @Test
    void replacementUsesTheLockedProfileAndFixedClock() {
        UUID userId = UUID.randomUUID();
        UserProfile profile = mock(UserProfile.class);
        List<String> interests = List.of("Hiking", "Coffee");
        when(profiles.findForUpdate(userId)).thenReturn(Optional.of(profile));

        service.replaceProfile(userId, "Updated Name", "Updated bio", interests);

        verify(profile).replace("Updated Name", "Updated bio", interests, NOW);
    }

    @Test
    void replacementTranslatesDomainValidationFailure() {
        UUID userId = UUID.randomUUID();
        UserProfile profile = mock(UserProfile.class);
        when(profiles.findForUpdate(userId)).thenReturn(Optional.of(profile));
        doThrow(new IllegalArgumentException("Interests must be unique ignoring case"))
                .when(profile)
                .replace("Profile", null, List.of("Hiking", "hiking"), NOW);

        assertThatThrownBy(() -> service.replaceProfile(
                userId,
                "Profile",
                null,
                List.of("Hiking", "hiking")
        ))
                .isInstanceOf(InvalidUserProfileException.class)
                .hasMessage("Interests must be unique ignoring case");
    }

    @Test
    void missingProfileReturnsTypedNotFoundFailure() {
        UUID userId = UUID.randomUUID();
        when(profiles.findForDisplay(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProfile(userId))
                .isInstanceOf(UserProfileNotFoundException.class)
                .hasMessage("User profile not found");
    }
}
