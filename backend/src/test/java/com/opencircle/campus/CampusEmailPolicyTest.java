package com.opencircle.campus;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CampusEmailPolicyTest {

    private CampusEmailPolicy policyRequiring(String suffix) {
        CampusProperties properties = new CampusProperties();
        properties.setRequiredEmailSuffix(suffix);
        return new CampusEmailPolicy(properties);
    }

    @Test
    void requiresEduByDefault() {
        CampusEmailPolicy policy = new CampusEmailPolicy(new CampusProperties());

        assertThatCode(() -> policy.requireCampusEmail("jane@sfsu.edu")).doesNotThrowAnyException();
        assertThatThrownBy(() -> policy.requireCampusEmail("jane@gmail.com"))
                .isInstanceOf(InvalidCampusEmailException.class)
                .hasMessage("Use your school email address (it must end in .edu)");
    }

    @Test
    void acceptsSubdomainsAndIgnoresCase() {
        CampusEmailPolicy policy = policyRequiring(".edu");

        assertThatCode(() -> policy.requireCampusEmail("Jane@Student.SFSU.EDU")).doesNotThrowAnyException();
    }

    @Test
    void rejectsLookalikeDomains() {
        CampusEmailPolicy policy = policyRequiring(".edu");

        assertThatThrownBy(() -> policy.requireCampusEmail("jane@sfsu.edu.example.com"))
                .isInstanceOf(InvalidCampusEmailException.class);
        assertThatThrownBy(() -> policy.requireCampusEmail("jane@notedu"))
                .isInstanceOf(InvalidCampusEmailException.class);
    }

    @Test
    void aBlankSuffixAllowsAnyEmail() {
        CampusEmailPolicy policy = policyRequiring("");

        assertThatCode(() -> policy.requireCampusEmail("jane@gmail.com")).doesNotThrowAnyException();
    }
}
