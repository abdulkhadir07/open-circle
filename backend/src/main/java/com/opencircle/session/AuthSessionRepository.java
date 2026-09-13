package com.opencircle.session;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select session
            from SessionRefreshToken token
            join token.session session
            where token.tokenHash = :tokenHash
            """)
    Optional<AuthSession> findForUpdateByRefreshTokenHash(@Param("tokenHash") String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update AuthSession session
            set session.revokedAt = :revokedAt,
                session.revocationReason = :reason
            where session.user.id = :userId
              and session.revokedAt is null
            """)
    int revokeAllActiveByUserId(
            @Param("userId") UUID userId,
            @Param("revokedAt") Instant revokedAt,
            @Param("reason") SessionRevocationReason reason
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from AuthSession session
            where session.expiresAt < :expiredBefore
               or session.revokedAt < :revokedBefore
            """)
    int deleteExpiredOrRevokedBefore(
            @Param("expiredBefore") Instant expiredBefore,
            @Param("revokedBefore") Instant revokedBefore
    );
}
