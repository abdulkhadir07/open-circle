package com.opencircle.location;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NominatimLocationResolverTest {

    @Test
    void resolveReturnsCityStateAndCountryFromCoordinates() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        NominatimLocationResolver resolver = new NominatimLocationResolver(restClientBuilder, locationProperties());

        expectReverseLookup(server, "37.7749", "-122.4194")
                .andRespond(withSuccess("""
                        {
                          "address": {
                            "city": "San Francisco",
                            "state": "California",
                            "country": "USA"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        ResolvedLocation location = resolver.resolve(
                BigDecimal.valueOf(37.7749),
                BigDecimal.valueOf(-122.4194)
        );

        assertThat(location.city()).isEqualTo("San Francisco");
        assertThat(location.stateRegion()).isEqualTo("California");
        assertThat(location.country()).isEqualTo("USA");

        server.verify();
    }

    @Test
    void resolveUsesTownWhenCityIsMissing() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        NominatimLocationResolver resolver = new NominatimLocationResolver(restClientBuilder, locationProperties());

        expectReverseLookup(server, "13.4549", "-16.579")
                .andRespond(withSuccess("""
                        {
                          "address": {
                            "town": "Brikama",
                            "country": "The Gambia"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        ResolvedLocation location = resolver.resolve(
                BigDecimal.valueOf(13.4549),
                BigDecimal.valueOf(-16.5790)
        );

        assertThat(location.city()).isEqualTo("Brikama");
        assertThat(location.stateRegion()).isNull();
        assertThat(location.country()).isEqualTo("The Gambia");

        server.verify();
    }

    @Test
    void resolveThrowsBadRequestWhenCoordinatesDoNotResolveToCityAndCountry() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        NominatimLocationResolver resolver = new NominatimLocationResolver(restClientBuilder, locationProperties());

        expectReverseLookup(server, "0.0", "0.0")
                .andRespond(withSuccess("""
                        {
                          "address": {
                            "country": "Atlantic Ocean"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> resolver.resolve(
                BigDecimal.valueOf(0.0000),
                BigDecimal.valueOf(0.0000)
        ))
                .isInstanceOf(LocationResolutionException.class)
                .hasMessage("Unable to verify location from coordinates");

        server.verify();
    }

    @Test
    void resolveThrowsServiceUnavailableWhenNominatimFails() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        NominatimLocationResolver resolver = new NominatimLocationResolver(restClientBuilder, locationProperties());

        expectReverseLookup(server, "37.7749", "-122.4194")
                .andRespond(withServerError());

        assertThatThrownBy(() -> resolver.resolve(
                BigDecimal.valueOf(37.7749),
                BigDecimal.valueOf(-122.4194)
        ))
                .isInstanceOf(LocationServiceUnavailableException.class)
                .hasMessage("Location verification is temporarily unavailable, please try again");

        server.verify();
    }

    private ResponseActions expectReverseLookup(
            MockRestServiceServer server,
            String latitude,
            String longitude
    ) {
        return server.expect(requestTo(startsWith("https://nominatim.test/reverse")))
                .andExpect(header(HttpHeaders.USER_AGENT, "OpenCircleTest/1.0"))
                .andExpect(queryParam("format", "jsonv2"))
                .andExpect(queryParam("lat", latitude))
                .andExpect(queryParam("lon", longitude))
                .andExpect(queryParam("zoom", "10"))
                .andExpect(queryParam("addressdetails", "1"))
                .andExpect(queryParam("accept-language", "en"));
    }

    private LocationProperties locationProperties() {
        LocationProperties properties = new LocationProperties();
        properties.setNominatimBaseUrl("https://nominatim.test");
        properties.setUserAgent("OpenCircleTest/1.0");
        return properties;
    }
}