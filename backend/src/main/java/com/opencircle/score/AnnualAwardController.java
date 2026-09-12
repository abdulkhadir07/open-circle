package com.opencircle.score;

import com.opencircle.profileimage.ProfileImageQueryService;
import com.opencircle.profileimage.ProfileImageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/awards")
public class AnnualAwardController {

    private final AnnualAwardService awardService;
    private final ProfileImageQueryService profileImageQueryService;

    AnnualAwardController(
            AnnualAwardService awardService,
            ProfileImageQueryService profileImageQueryService
    ) {
        this.awardService = awardService;
        this.profileImageQueryService = profileImageQueryService;
    }

    @GetMapping("/{year}")
    public AnnualAwardResponse getAward(@PathVariable int year) {
        FinalizedAnnualAward award = awardService.getAward(year);
        Set<UUID> winnerIds = award.winners().stream()
                .map(AnnualAwardWinner::userId)
                .collect(Collectors.toSet());
        Map<UUID, ProfileImageResponse> profileImages =
                profileImageQueryService.getProfileImagesByUserIds(winnerIds);

        List<AnnualAwardWinnerResponse> winners = award.winners().stream()
                .map(winner -> AnnualAwardWinnerResponse.from(
                        winner,
                        profileImages.get(winner.userId())
                ))
                .toList();

        return new AnnualAwardResponse(
                award.seasonYear(),
                "Circle Champion " + award.seasonYear(),
                award.finalizedAt(),
                winners
        );
    }
}
