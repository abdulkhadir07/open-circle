package com.opencircle.user;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    @EntityGraph(attributePaths = {"user", "interests"})
    @Query("select profile from UserProfile profile where profile.userId = :userId")
    Optional<UserProfile> findForDisplay(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select profile from UserProfile profile where profile.userId = :userId")
    Optional<UserProfile> findForUpdate(UUID userId);
}
