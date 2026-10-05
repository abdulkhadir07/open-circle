package com.opencircle.ai;

import java.util.List;

record FeedInsightsResponse(String digest, List<FeedReason> reasons, boolean aiGenerated) {
}
