package com.opencircle.verification;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
class VerificationCodeGenerator {

    private static final int MIN_CODE = 100_000;
    private static final int MAX_CODE_EXCLUSIVE = 1_000_000;

    private final SecureRandom random = new SecureRandom();

    String generate() {
        int code = random.nextInt(MIN_CODE, MAX_CODE_EXCLUSIVE);
        return String.valueOf(code);
    }
}
