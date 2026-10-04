package com.opencircle.campus;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CampusTest {

    @Test
    void campusIsTheDomainOfAPlainEmail() {
        assertThat(Campus.fromEmail("jane@sfsu.edu")).isEqualTo("sfsu.edu");
    }

    @Test
    void subdomainsBelongToTheSameCampus() {
        assertThat(Campus.fromEmail("jane@student.sfsu.edu")).isEqualTo("sfsu.edu");
        assertThat(Campus.fromEmail("jane@mail.cs.sfsu.edu")).isEqualTo("sfsu.edu");
    }

    @Test
    void campusIsLowercasedAndTrimmed() {
        assertThat(Campus.fromEmail("Jane@Student.SFSU.EDU")).isEqualTo("sfsu.edu");
        assertThat(Campus.fromEmail("jane@sfsu.edu ")).isEqualTo("sfsu.edu");
    }

    @Test
    void aSingleLabelDomainIsKeptAsIs() {
        assertThat(Campus.fromEmail("jane@localhost")).isEqualTo("localhost");
    }

    @Test
    void emailsWithoutADomainAreRejected() {
        assertThatThrownBy(() -> Campus.fromEmail("no-at-sign"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Campus.fromEmail("trailing@"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Campus.fromEmail(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
