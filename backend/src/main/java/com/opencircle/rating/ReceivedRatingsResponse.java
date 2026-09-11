package com.opencircle.rating;

import java.util.List;

public record ReceivedRatingsResponse(
        List<ReceivedRatingResponse> ratings,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
