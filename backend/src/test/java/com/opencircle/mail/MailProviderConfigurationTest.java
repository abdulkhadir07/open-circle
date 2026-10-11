package com.opencircle.mail;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MailProviderConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(MailTestConfiguration.class)
            .withPropertyValues(
                    "app.mail.from=no-reply@opencircle.test",
                    "app.mail.from-name=OpenCircle"
            );

    @Test
    void usesSmtpByDefaultForLocalMailpitDevelopment() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(MailService.class);
            assertThat(context.getBean(MailService.class)).isInstanceOf(SmtpMailService.class);
        });
    }

    @Test
    void usesBrevoWhenTheProductionProviderIsSelected() {
        contextRunner
                .withPropertyValues(
                        "app.mail.provider=brevo",
                        "app.mail.brevo.api-key=test-api-key",
                        "app.mail.brevo.base-url=https://brevo.test"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(MailService.class);
                    assertThat(context.getBean(MailService.class)).isInstanceOf(BrevoMailService.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(MailProperties.class)
    @Import({SmtpMailService.class, BrevoMailService.class})
    static class MailTestConfiguration {

        @Bean
        JavaMailSender javaMailSender() {
            return mock(JavaMailSender.class);
        }
    }
}
