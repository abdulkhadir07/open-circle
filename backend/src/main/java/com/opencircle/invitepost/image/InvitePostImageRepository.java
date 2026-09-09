package com.opencircle.invitepost.image;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface InvitePostImageRepository extends JpaRepository<InvitePostImage, UUID> {

    long countByInvitePostId(UUID invitePostId);

    List<InvitePostImage> findByInvitePostIdInOrderByInvitePostIdAscDisplayOrderAsc(
            Collection<UUID> invitePostIds
    );
}
