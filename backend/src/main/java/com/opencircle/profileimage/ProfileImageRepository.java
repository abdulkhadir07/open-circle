package com.opencircle.profileimage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ProfileImageRepository extends JpaRepository<ProfileImage, UUID> {

    Optional<ProfileImage> findByUser_Id(UUID userId);

    List<ProfileImage> findAllByUser_IdIn(Collection<UUID> userIds);
}
