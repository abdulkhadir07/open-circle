package com.opencircle;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public abstract class AbstractIntegrationTest {

    @SuppressWarnings("resource")
    private static final GenericContainer<?> postgres =
            new GenericContainer<>("postgres:16-alpine")
                    .withExposedPorts(5432)
                    .withEnv("POSTGRES_DB", "opencircle_test")
                    .withEnv("POSTGRES_USER", "opencircle")
                    .withEnv("POSTGRES_PASSWORD", "opencircle")
                    .waitingFor(Wait.forListeningPort());
    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", AbstractIntegrationTest::jdbcUrl);
        registry.add("spring.datasource.username", () -> "opencircle");
        registry.add("spring.datasource.password", () -> "opencircle");
    }

    private static String jdbcUrl() {
        return "jdbc:postgresql://%s:%d/opencircle_test".formatted(
                postgres.getHost(),
                postgres.getMappedPort(5432)
        );
    }
}
