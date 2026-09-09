package com.opencircle.invitepost.image;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.invite-post.images")
public class InvitePostImageProperties {

    @Positive
    @Max(4)
    private int maxImagesPerPost;

    public int getMaxImagesPerPost() {
        return maxImagesPerPost;
    }

    public void setMaxImagesPerPost(int maxImagesPerPost) {
        this.maxImagesPerPost = maxImagesPerPost;
    }
}
