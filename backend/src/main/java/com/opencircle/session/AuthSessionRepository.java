package com.opencircle.session;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select session
            from AuthSession session
            where session.id = :sessionId
              and session.user.id = :userId
            """)
    Optional<AuthSession> findOwnedForUpdate(
            @Param("sessionId") UUID sessionId,
            @Param("userId") UUID userId
    );

    @Query("""
            select session.id as id,
                   session.userAgent as userAgent,
                   session.createdAt as createdAt,
                   session.lastUsedAt as lastUsedAt,
                   token.expiresAt as inactiveAt,
                   session.expiresAt as expiresAt
            from SessionRefreshToken token
            join token.session session
            where session.user.id = :userId
              and session.revokedAt is null
              and session.expiresAt > :now
              and token.usedAt is null
              and token.expiresAt > :now
            order by case when session.id = :currentSessionId then 0 else 1 end,
                     session.lastUsedAt desc,
                     session.id
            """)
    List<ActiveSessionProjection> findActiveByUserId(
            @Param("userId") UUID userId,
            @Param("currentSessionId") UUID currentSessionId,
            @Param("now") Instant now
    );

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
            update AuthSession session
            set session.revokedAt = :revokedAt,
                session.revocationReason = :reason
            where session.user.id = :userId
              and session.id <> :currentSessionId
              and session.revokedAt is null
            """)
    int revokeOtherActiveByUserId(
            @Param("userId") UUID userId,
            @Param("currentSessionId") UUID currentSessionId,
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
