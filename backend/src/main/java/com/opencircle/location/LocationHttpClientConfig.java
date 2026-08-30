package com.opencircle.location;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
class LocationHttpClientConfig {

    @Bean
    RestClient.Builder restClientBuilder() {
        // Provides the HTTP client builder used by the Nominatim location resolver.
        return RestClient.builder();
    }
}
