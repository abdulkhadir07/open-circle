package com.opencircle.passwordreset;

import com.opencircle.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, UUID> {

    List<PasswordResetCode> findByUserAndUsedAtIsNull(AppUser user);

    Optional<PasswordResetCode> findFirstByUserAndUsedAtIsNullOrderByCreatedAtDesc(AppUser user);
}
