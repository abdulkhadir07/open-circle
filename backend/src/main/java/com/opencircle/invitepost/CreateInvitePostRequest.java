package com.opencircle.invitepost;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

record CreateInvitePostRequest(
        @NotBlank(message = "Content is required")
        @Size(max = 500)
        String content,

        @NotNull(message = "Invite type is required")
        InviteType inviteType,

        @Positive(message = "Total capacity must be positive")
        Integer totalCapacity,

        @NotNull(message = "Location scope is required")
        LocationScope locationScope,

        @Size(max = InvitePost.MAX_TAGS, message = "A post can have at most 5 tags")
        List<@Size(max = InvitePost.MAX_TAG_LENGTH, message = "Tags must not exceed 30 characters") String> tags
) {
}