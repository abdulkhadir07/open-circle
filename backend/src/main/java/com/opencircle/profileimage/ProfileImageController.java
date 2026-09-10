package com.opencircle.profileimage;

import com.opencircle.security.CurrentUserProvider;
import com.opencircle.user.AppUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/users/me/profile-image")
class ProfileImageController {

    private final CurrentUserProvider currentUserProvider;
    private final ProfileImageService profileImageService;

    ProfileImageController(
            CurrentUserProvider currentUserProvider,
            ProfileImageService profileImageService
    ) {
        this.currentUserProvider = currentUserProvider;
        this.profileImageService = profileImageService;
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ProfileImageResponse uploadOrReplace(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("file") MultipartFile file
    ) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);

        try {
            return profileImageService.uploadOrReplace(
                    currentUser,
                    new ProfileImageUpload(
                            file.getOriginalFilename(),
                            file.getContentType(),
                            file.getSize(),
                            file.getInputStream()
                    )
            );
        } catch (IOException exception) {
            throw new InvalidProfileImageException("Unable to read uploaded image");
        }
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal Jwt jwt) {
        profileImageService.delete(currentUserProvider.getCurrentUser(jwt));
    }
}
