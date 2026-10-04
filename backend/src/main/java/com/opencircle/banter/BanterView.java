package com.opencircle.banter;

// A banter with the numbers the board shows next to it.
record BanterView(Banter banter, long likeCount, long replyCount, boolean likedByMe) {
}
