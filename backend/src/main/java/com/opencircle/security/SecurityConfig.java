package com.opencircle.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;


@Configuration
class SecurityConfig {

    private final CorsProperties corsProperties;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    SecurityConfig(
            CorsProperties corsProperties,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler
    ) {
        this.corsProperties = corsProperties;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // The API uses JWT authentication, so CSRF protection for browser sessions is not needed.
                .csrf(csrf -> csrf.disable())

                // Applies the configured frontend origins, methods, and headers to all API requests.
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Keeps the backend stateless by preventing Spring Security from creating HTTP sessions.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Allows public auth flows while requiring a valid JWT for every other endpoint.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/signup").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/verify-email").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/resend-verification").permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/auth/forgot-password").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/reset-password").permitAll()

                        .requestMatchers(HttpMethod.PATCH, "/api/engagements/*/accept").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/engagements/*/decline").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/engagements/*/hold").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/engagements/*/withdraw").authenticated()

                        .requestMatchers(HttpMethod.POST, "/api/invite-posts/*/engagements").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/invite-posts/*/engagements").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/invite-posts").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/invite-posts/local").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/invite-posts/global").authenticated()

                        .requestMatchers(HttpMethod.GET, "/api/chat-rooms").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/chat-rooms/*/messages").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/chat-rooms/*/messages").authenticated()

                        .anyRequest().authenticated()
                )

                // Validates bearer tokens and converts the JWT role claim into Spring Security authorities.
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )

                // Returns consistent JSON error responses for missing, invalid, or unauthorized tokens.
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .build();
    }

    private UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Restricts cross-origin requests to the frontend origins configured in application properties.
        config.setAllowedOrigins(corsProperties.getAllowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        // Reads the custom role claim from the JWT and exposes it as a ROLE_* authority.
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String role = jwt.getClaimAsString("role");

            if (role == null) {
                return List.of();
            }

            return List.of(new SimpleGrantedAuthority("ROLE_" + role));
        });

        return converter;
    }
}
