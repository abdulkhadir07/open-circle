package com.opencircle.invitepost.image;

import com.opencircle.security.CurrentUserProvider;
import com.opencircle.user.AppUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/invite-posts/{postId}/images")
class InvitePostImageController {

    private final CurrentUserProvider currentUserProvider;
    private final InvitePostImageService imageService;

    InvitePostImageController(
            CurrentUserProvider currentUserProvider,
            InvitePostImageService imageService
    ) {
        this.currentUserProvider = currentUserProvider;
        this.imageService = imageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    InvitePostImageResponse uploadImage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID postId,
            @RequestParam("file") MultipartFile file
    ) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);

        try {
            return imageService.uploadImage(
                    currentUser,
                    postId,
                    new InvitePostImageUpload(
                            file.getOriginalFilename(),
                            file.getContentType(),
                            file.getSize(),
                            file.getInputStream()
                    )
            );
        } catch (IOException exception) {
            throw new InvalidInvitePostImageException("Unable to read uploaded image");
        }
    }
}
