package com.opencircle.verification;

import com.opencircle.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, UUID> {

    List<EmailVerificationCode> findByUserAndUsedAtIsNull(AppUser user);

    Optional<EmailVerificationCode> findFirstByUserAndUsedAtIsNullOrderByCreatedAtDesc(AppUser user);
}
