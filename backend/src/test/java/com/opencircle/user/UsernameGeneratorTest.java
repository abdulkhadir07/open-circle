package com.opencircle.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsernameGeneratorTest {

    @Mock
    private UserRepository users;

    @Test
    void generatesUsernameMatchingExpectedFormat() {
        when(users.existsByUsernameIgnoreCase(anyString())).thenReturn(false);

        UsernameGenerator generator = new UsernameGenerator(users);

        assertThat(generator.generate()).matches("^[a-z]+_[a-z]+_\\d{4}$");
    }

    @Test
    void retriesWhenGeneratedUsernameAlreadyExists() {
        when(users.existsByUsernameIgnoreCase(anyString()))
                .thenReturn(true)
                .thenReturn(false);

        UsernameGenerator generator = new UsernameGenerator(users);

        assertThat(generator.generate()).isNotBlank();
        verify(users, atLeast(2)).existsByUsernameIgnoreCase(anyString());
    }

    @Test
    void throwsWhenUniqueUsernameCannotBeGenerated() {
        when(users.existsByUsernameIgnoreCase(anyString())).thenReturn(true);

        UsernameGenerator generator = new UsernameGenerator(users);

        assertThatThrownBy(generator::generate)
                .isInstanceOf(IllegalStateException.class);
    }
}
