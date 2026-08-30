package com.opencircle.location;

import com.opencircle.AbstractIntegrationTest;
import com.opencircle.security.JwtService;
import com.opencircle.user.AppUser;
import com.opencircle.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LocationControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private LocationResolver locationResolver;

    @Test
    void verifyMyLocationStoresResolvedLocationForAuthenticatedUser() throws Exception {
        AppUser user = createUser(
                "location.user@example.com",
                "+14155550400"
        );

        when(locationResolver.resolve(
                java.math.BigDecimal.valueOf(37.7749),
                java.math.BigDecimal.valueOf(-122.4194)
        )).thenReturn(new ResolvedLocation("San Francisco", "California", "USA"));

        String token = jwtService.generateToken(user);

        mockMvc.perform(put("/api/users/me/location")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "latitude": 37.7749,
                                  "longitude": -122.4194
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verifiedCity").value("San Francisco"))
                .andExpect(jsonPath("$.verifiedStateRegion").value("California"))
                .andExpect(jsonPath("$.verifiedCountry").value("USA"))
                .andExpect(jsonPath("$.locationVerifiedAt").exists())
                .andExpect(jsonPath("$.locationSource").value("DEVICE"));
    }

    @Test
    void verifyMyLocationRejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(put("/api/users/me/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "latitude": 37.7749,
                                  "longitude": -122.4194
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void verifyMyLocationRejectsInvalidCoordinates() throws Exception {
        AppUser user = createUser(
                "invalid.location@example.com",
                "+14155550401"
        );

        String token = jwtService.generateToken(user);

        mockMvc.perform(put("/api/users/me/location")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "latitude": 100.0,
                                  "longitude": -200.0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.latitude").exists())
                .andExpect(jsonPath("$.fieldErrors.longitude").exists());
    }

    @Test
    void verifyMyLocationReturnsBadRequestWhenCoordinatesCannotBeResolved() throws Exception {
        AppUser user = createUser(
                "unresolved.location@example.com",
                "+14155550402"
        );

        when(locationResolver.resolve(
                java.math.BigDecimal.valueOf(0.0),
                java.math.BigDecimal.valueOf(0.0)
        )).thenThrow(new LocationResolutionException());

        String token = jwtService.generateToken(user);

        mockMvc.perform(put("/api/users/me/location")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "latitude": 0.0,
                                  "longitude": 0.0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unable to verify location from coordinates"));
    }

    @Test
    void verifyMyLocationReturnsServiceUnavailableWhenResolverFails() throws Exception {
        AppUser user = createUser(
                "resolver.down@example.com",
                "+14155550403"
        );

        when(locationResolver.resolve(
                java.math.BigDecimal.valueOf(37.7749),
                java.math.BigDecimal.valueOf(-122.4194)
        )).thenThrow(new LocationServiceUnavailableException());

        String token = jwtService.generateToken(user);

        mockMvc.perform(put("/api/users/me/location")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "latitude": 37.7749,
                                  "longitude": -122.4194
                                }
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Location verification is temporarily unavailable, please try again"));
    }

    private AppUser createUser(String email, String phoneNumber) {
        return userService.createUser(
                "Location",
                "User",
                email,
                "hashed-password",
                phoneNumber,
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                null,
                "USA"
        );
    }
}