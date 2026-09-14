package com.opencircle.accountsettings;

import com.opencircle.user.AppUser;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface EmailChangeRequestRepository extends JpaRepository<EmailChangeRequest, UUID> {

    Optional<EmailChangeRequest> findByUserAndUsedAtIsNull(AppUser user);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select request
            from EmailChangeRequest request
            where request.user.id = :userId
              and request.usedAt is null
            """)
    Optional<EmailChangeRequest> findActiveForUpdate(@Param("userId") UUID userId);
}
