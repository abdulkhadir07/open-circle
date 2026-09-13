package com.opencircle.session;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SessionRefreshTokenRepository extends JpaRepository<SessionRefreshToken, UUID> {

    Optional<SessionRefreshToken> findByTokenHash(String tokenHash);
}
