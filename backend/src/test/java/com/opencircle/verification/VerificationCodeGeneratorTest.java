package com.opencircle.verification;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class VerificationCodeGeneratorTest {

    private final VerificationCodeGenerator generator = new VerificationCodeGenerator();

    @Test
    void generateReturnsSixDigitCode() {
        String code = generator.generate();

        assertThat(code).matches("\\d{6}");
    }
}
