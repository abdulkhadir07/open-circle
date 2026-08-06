package com.opencircle.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OtpCodeGeneratorTest {

    private final OtpCodeGenerator generator = new OtpCodeGenerator();

    @Test
    void generateReturnsSixDigitCode() {
        String code = generator.generate();

        assertThat(code).matches("\\d{6}");
    }
}
