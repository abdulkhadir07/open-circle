package com.opencircle.score;

import com.opencircle.profileimage.ProfileImageQueryService;
import com.opencircle.profileimage.ProfileImageResponse;
import com.opencircle.security.CurrentUserProvider;
import com.opencircle.user.AppUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ScoreController {

    private final CurrentUserProvider currentUserProvider;
    private final ScoreService scoreService;
    private final ScoreboardService scoreboardService;
    private final ProfileImageQueryService profileImageQueryService;

    ScoreController(
            CurrentUserProvider currentUserProvider,
            ScoreService scoreService,
            ScoreboardService scoreboardService,
            ProfileImageQueryService profileImageQueryService
    ) {
        this.currentUserProvider = currentUserProvider;
        this.scoreService = scoreService;
        this.scoreboardService = scoreboardService;
        this.profileImageQueryService = profileImageQueryService;
    }

    @GetMapping("/users/me/score")
    public ScoreSummaryResponse getCurrentUserScore(@AuthenticationPrincipal Jwt jwt) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);
        return ScoreSummaryResponse.from(
                currentUser.getId(),
                scoreService.getCurrentScore(currentUser)
        );
    }

    @GetMapping("/scoreboard")
    public ScoreboardResponse getScoreboard() {
        CurrentScoreboard scoreboard = scoreboardService.getCurrentScoreboard();
        Set<UUID> userIds = scoreboard.entries().stream()
                .map(RankedScoreboardEntry::userId)
                .collect(Collectors.toSet());
        Map<UUID, ProfileImageResponse> profileImages =
                profileImageQueryService.getProfileImagesByUserIds(userIds);

        List<ScoreboardEntryResponse> entries = scoreboard.entries().stream()
                .map(entry -> ScoreboardEntryResponse.from(
                        entry,
                        profileImages.get(entry.userId())
                ))
                .toList();

        return new ScoreboardResponse(scoreboard.seasonYear(), entries);
    }
}
