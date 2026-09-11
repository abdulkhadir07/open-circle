package com.opencircle.rating;

import com.opencircle.engagement.EngagementRequest;
import com.opencircle.user.AppUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class RatingEnrollmentService {

    private final RatingObligationRepository obligations;

    RatingEnrollmentService(RatingObligationRepository obligations) {
        this.obligations = obligations;
    }

    @Transactional
    public void enrollAcceptedEngagement(EngagementRequest engagementRequest, Instant acceptedAt) {
        AppUser poster = engagementRequest.getInvitePost().getPoster();
        AppUser requester = engagementRequest.getRequester();

        obligations.saveAll(List.of(
                new RatingObligation(engagementRequest, poster, requester, acceptedAt),
                new RatingObligation(engagementRequest, requester, poster, acceptedAt)
        ));
    }
}
